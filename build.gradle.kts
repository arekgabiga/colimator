plugins {
    // Trick: Apply plugins with 'apply false' in the root project so subprojects can use them via alias
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

group = "com.colimator.app"
version = "1.0.0"
