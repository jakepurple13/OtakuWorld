package com.programmersbox.otakuworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.BottomDrawer
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import com.google.android.material.composethemeadapter.MdcTheme

class MainActivity : ComponentActivity() {
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MdcTheme {

                BottomDrawer(
                    drawerContent = {
                        Scaffold {
                            Column {
                                repeat(10) {
                                    Text("Hello $it")
                                }
                            }
                        }
                    }
                ) {
                    Scaffold {
                        Column {
                            repeat(10) {
                                Text("Hello $it")
                            }
                        }
                    }
                }

            }
        }
    }
}