package com.programmersbox.kmpextensionloader

import com.googlecode.d2j.DexConstants
import com.googlecode.d2j.Field
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
 *
 * The same elision breaks constructors: when R8 drops a superclass's trivial constructor, a subclass
 * constructor calls `invoke-direct this, Ljava/lang/Object;-><init>()V` (or another ancestor's), which the
 * JVM rejects ("Bad <init> method call": only the direct superclass constructor may be called on `this`).
 * Such super calls are pointed at the direct superclass, which gets the same pass-through constructor.
 */
internal class ElidedConstructorFix private constructor(
    private val reader: BaseDexFileReader,
    /** (class to call the constructor of instead, constructor call) pairs to retarget. */
    private val accepted: Set<Pair<String, Method>>,
    /** In-dex class descriptor -> superclass descriptor. */
    private val supers: Map<String, String?>,
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
                        override fun visitCode(): DexCodeVisitor? =
                            super.visitCode()?.let { code ->
                                NewInstanceTracker(code, superCallOf(className, accessFlags, method, supers)) { type, call -> type to call in accepted }
                            }
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

    /** A constructor's own super call: `this` is the first of the last [insSize] registers. */
    private class SuperCall(val self: String, val directSuper: String, val insSize: Int)

    /**
     * Tracks which registers hold a fresh `new-instance` and finds `invoke-direct X.<init>` calls on them
     * where X is not the instantiated type. In a constructor ([superCall] set) it also finds `invoke-direct
     * X.<init>` on `this` where X is not the direct superclass, reported as (direct superclass, call).
     * [rewrite] decides whether such a call is retargeted to the reported type; [onCandidate] sees every one.
     * Any other write to a register forgets it (as a fresh instance or as `this`), so stale entries never match.
     */
    private class NewInstanceTracker(
        cv: DexCodeVisitor?,
        private val superCall: SuperCall? = null,
        private val onCandidate: (type: String, call: Method) -> Unit = { _, _ -> },
        private val rewrite: (type: String, call: Method) -> Boolean,
    ) : DexCodeVisitor(cv) {
        private val newInstances = mutableMapOf<Int, String>()
        private var thisReg = -1

        /** [reg] is written: it no longer holds a fresh instance, or `this`. */
        private fun forget(reg: Int) {
            newInstances.remove(reg)
            if (reg == thisReg) thisReg = -1
        }

        override fun visitRegister(total: Int) {
            if (superCall != null) thisReg = total - superCall.insSize
            super.visitRegister(total)
        }

        override fun visitTypeStmt(op: Op, a: Int, b: Int, type: String) {
            forget(a)
            if (op == Op.NEW_INSTANCE) newInstances[a] = type
            super.visitTypeStmt(op, a, b, type)
        }

        override fun visitStmt1R(op: Op, reg: Int) {
            forget(reg)
            super.visitStmt1R(op, reg)
        }

        override fun visitStmt2R(op: Op, a: Int, b: Int) {
            forget(a)
            super.visitStmt2R(op, a, b)
        }

        override fun visitStmt2R1N(op: Op, distReg: Int, srcReg: Int, content: Int) {
            forget(distReg)
            super.visitStmt2R1N(op, distReg, srcReg, content)
        }

        override fun visitStmt3R(op: Op, a: Int, b: Int, c: Int) {
            forget(a)
            super.visitStmt3R(op, a, b, c)
        }

        override fun visitConstStmt(op: Op, ra: Int, value: Any?) {
            forget(ra)
            super.visitConstStmt(op, ra, value)
        }

        override fun visitFieldStmt(op: Op, a: Int, b: Int, field: Field) {
            forget(a)
            super.visitFieldStmt(op, a, b, field)
        }

        override fun visitMethodStmt(op: Op, args: IntArray, method: Method) {
            val type = if ((op == Op.INVOKE_DIRECT || op == Op.INVOKE_DIRECT_RANGE) && method.name == "<init>" && args.isNotEmpty()) {
                newInstances.remove(args[0]) ?: superCall?.takeIf { args[0] == thisReg && method.owner != it.self }?.directSuper
            } else {
                null
            }
            if (type != null && type != method.owner) {
                onCandidate(type, method)
                if (rewrite(type, method)) {
                    super.visitMethodStmt(op, args, Method(type, "<init>", method.parameterTypes, "V"))
                    return
                }
            }
            super.visitMethodStmt(op, args, method)
        }
    }

    companion object {
        private const val OBJECT = "Ljava/lang/Object;"

        /** For an instance constructor of an in-dex class, what [NewInstanceTracker] needs to spot its super call. */
        private fun superCallOf(className: String, accessFlags: Int, method: Method, supers: Map<String, String?>): SuperCall? {
            if (method.name != "<init>" || accessFlags and DexConstants.ACC_STATIC != 0) return null
            val directSuper = supers[className] ?: return null
            return SuperCall(className, directSuper, 1 + method.parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 })
        }

        /**
         * True when [owner] is a superclass of [type]: walking [supers] (in-dex class -> superclass) from [type]
         * reaches [owner]. Once the walk leaves the dex, only the first out-of-dex superclass or Object count.
         */
        internal fun isAncestor(supers: Map<String, String?>, type: String, owner: String): Boolean {
            var current = type
            repeat(supers.size) {
                val superName = supers[current] ?: return false
                if (superName == owner) return true
                if (superName !in supers) return owner == OBJECT
                current = superName
            }
            return false // cycle
        }

        /** Scans [reader] once and returns a reader that applies the fix, or [reader] itself when nothing needs it. */
        fun wrap(reader: BaseDexFileReader): BaseDexFileReader {
            val supers = mutableMapOf<String, String?>()
            val constructors = mutableMapOf<String, MutableSet<String>>()
            val candidates = linkedSetOf<Pair<String, Method>>()

            reader.accept(object : DexFileVisitor() {
                override fun visit(accessFlags: Int, className: String, superClass: String?, interfaceNames: Array<String>?): DexClassVisitor {
                    supers[className] = superClass
                    return object : DexClassVisitor() {
                        override fun visitMethod(accessFlags: Int, method: Method): DexMethodVisitor {
                            if (method.name == "<init>") constructors.getOrPut(className) { mutableSetOf() } += method.desc
                            return object : DexMethodVisitor() {
                                // supers is complete for this class: visit() ran before its methods.
                                override fun visitCode(): DexCodeVisitor = NewInstanceTracker(
                                    null,
                                    superCallOf(className, accessFlags, method, supers),
                                    onCandidate = { type, call -> candidates += type to call },
                                ) { _, _ -> false }
                            }
                        }
                    }
                }
            })

            val accepted = candidates.filterTo(linkedSetOf()) { (type, call) ->
                when {
                    type !in supers -> false // only classes defined in this dex can get a constructor
                    call.parameterTypes.any { it == "J" || it == "D" } -> {
                        println("DexConverter: not retargeting $call on new $type: wide parameters")
                        false
                    }
                    !isAncestor(supers, type, call.owner) -> {
                        println("DexConverter: not retargeting $call on new $type: ${call.owner} is not its superclass")
                        false
                    }
                    else -> true
                }
            }
            if (accepted.isEmpty()) return reader

            val additions = mutableMapOf<String, MutableList<Pair<Array<String>, String>>>()
            accepted.forEach { (type, call) ->
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
            return ElidedConstructorFix(reader, accepted, supers, additions)
        }
    }
}
