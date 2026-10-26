package androidx.core.app

import android.app.Activity

/** SDK supertype chain of AppCompatActivity. Desktop never creates one; abstract because Activity is. */
abstract class ComponentActivity : Activity()
