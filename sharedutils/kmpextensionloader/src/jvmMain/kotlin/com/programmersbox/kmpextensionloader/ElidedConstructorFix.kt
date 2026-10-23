package com.programmersbox.kmpextensionloader

import com.googlecode.d2j.DexConstants
import com.googlecode.d2j.Method
import com.googlecode.d2j.reader.BaseDexFileReader
import com.googlecode.d2j.reader.Op
import com.googlecode.d2j.visitors.DexClassVisitor
import com.googlecode.d2j.visitors.DexCodeVisitor
import com.googlecode.d2j.visitors.DexFileVisitor
import com.googlecode.d2j.visitors.DexMethodVisitor

/**
 * R8 removes trivial constructors and creates instances with `new-instance LFoo;` followed by
 * `invoke-direct Ljava/lang/Object;-><init>()V` (or another superclass constructor). ART accepts that;
 * dex2jar turns it into `new Object()`, so e.g. an extension's network interceptor arrives as a bare
 * Object and okhttp fails with a ClassCastException.
 *
 * This reader wrapper points such calls at the instantiated class and gives that class (and any
 * in-dex superclasses on the way up) a constructor that only calls its superclass constructor,
 * which is what the removed constructor did.
 */
internal class ElidedConstructorFix private constructor(
    private val reader: BaseDexFileReader,
    private val classes: Set<String>,
    /** Class descriptor -> constructors to add, each as (parameter types, superclass to call). */
    private val additions: Map<String, List<Pair<Array<String>, String>>>,
) : BaseDexFileReader by reader {

    override fun accept(dv: DexFileVisitor) = reader.accept(RewritingFile(dv))
    override fun accept(dv: DexFileVisitor, config: Int) = reader.accept(RewritingFile(dv), config)
    override fun accept(dv: DexFileVisitor, classIdx: Int, config: Int) = reader.accept(RewritingFile(dv), classIdx, config)

    private inner class RewritingFile(dv: DexFileVisitor) : DexFileVisitor(dv) {
        override fun visit(accessFlags: Int, className: String, superClass: String?, interfaceNames: Array<String>?): DexClassVisitor? {
            val cv = super.visit(accessFlags, className, superClass, interfaceNames) ?: return null
            return object : DexClassVisitor(cv) {
                override fun visitMethod(accessFlags: Int, method: Method): DexMethodVisitor? {
                    val mv = super.visitMethod(accessFlags, method) ?: return null
                    return object : DexMethodVisitor(mv) {
                        override fun visitCode(): DexCodeVisitor? = super.visitCode()?.let { RewritingCode({ type -> type in classes }, it) }
                    }
                }

                override fun visitEnd() {
                    additions[className].orEmpty().forEach { (params, superName) -> addConstructor(className, params, superName) }
                    super.visitEnd()
                }

                private fun addConstructor(owner: String, params: Array<String>, superName: String) {
                    val mv = super.visitMethod(DexConstants.ACC_PUBLIC or DexConstants.ACC_CONSTRUCTOR, Method(owner, "<init>", params, "V")) ?: return
                    mv.visitCode()?.run {
                        // Arguments sit in the last registers: this, then one register per (non-wide) parameter.
                        visitRegister(params.size + 1)
                        visitMethodStmt(Op.INVOKE_DIRECT, IntArray(params.size + 1) { it }, Method(superName, "<init>", params, "V"))
                        visitStmt0R(Op.RETURN_VOID)
                        visitEnd()
                    }
                    mv.visitEnd()
                }
            }
        }
    }

    /** Rewrites `invoke-direct Super.<init>` on a register that holds a fresh `new-instance` of an in-dex subclass. */
    private class RewritingCode(
        private val isDexClass: (String) -> Boolean,
        cv: DexCodeVisitor?,
        private val onRewrite: (type: String, call: Method) -> Unit = { _, _ -> },
    ) : DexCodeVisitor(cv) {
        private val newInstances = mutableMapOf<Int, String>()

        override fun visitTypeStmt(op: Op, a: Int, b: Int, type: String) {
            if (op == Op.NEW_INSTANCE) newInstances[a] = type
            super.visitTypeStmt(op, a, b, type)
        }

        override fun visitMethodStmt(op: Op, args: IntArray, method: Method) {
            val type = if ((op == Op.INVOKE_DIRECT || op == Op.INVOKE_DIRECT_RANGE) && method.name == "<init>" && args.isNotEmpty()) {
                newInstances.remove(args[0])
            } else {
                null
            }
            val rewrite = type != null && type != method.owner && isDexClass(type) &&
                method.parameterTypes.none { it == "J" || it == "D" }
            if (rewrite) {
                onRewrite(type, method)
                super.visitMethodStmt(op, args, Method(type, "<init>", method.parameterTypes, "V"))
            } else {
                super.visitMethodStmt(op, args, method)
            }
        }
    }

    companion object {
        /** Scans [reader] once and returns a reader that applies the fix, or [reader] itself when nothing needs it. */
        fun wrap(reader: BaseDexFileReader): BaseDexFileReader {
            val supers = mutableMapOf<String, String?>()
            val constructors = mutableMapOf<String, MutableSet<String>>()
            val needed = linkedSetOf<Pair<String, Method>>()

            reader.accept(object : DexFileVisitor() {
                override fun visit(accessFlags: Int, className: String, superClass: String?, interfaceNames: Array<String>?): DexClassVisitor {
                    supers[className] = superClass
                    return object : DexClassVisitor() {
                        override fun visitMethod(accessFlags: Int, method: Method): DexMethodVisitor {
                            if (method.name == "<init>") constructors.getOrPut(className) { mutableSetOf() } += method.desc
                            return object : DexMethodVisitor() {
                                override fun visitCode(): DexCodeVisitor =
                                    RewritingCode({ true }, null) { type, call -> needed += type to call }
                            }
                        }
                    }
                }
            })
            // Only classes defined in this dex can get a constructor.
            needed.retainAll { (type, _) -> type in supers }
            if (needed.isEmpty()) return reader

            val additions = mutableMapOf<String, MutableList<Pair<Array<String>, String>>>()
            needed.forEach { (type, call) ->
                var current = type
                while (true) {
                    val superName = supers[current] ?: break
                    val existing = constructors.getOrPut(current) { mutableSetOf() }
                    if (!existing.add(call.desc)) break
                    additions.getOrPut(current) { mutableListOf() } += call.parameterTypes to superName
                    // The JVM only allows calling the direct superclass constructor, so walk up to the original target.
                    if (superName == call.owner || superName !in supers) break
                    current = superName
                }
            }
            return ElidedConstructorFix(reader, supers.keys, additions)
        }
    }
}
