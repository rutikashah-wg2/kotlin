// RUN_PIPELINE_TILL: BACKEND
fun <X> test_1(a: X) {
    if (a is String?) {
        takeString(a!!)
    }
}

fun takeString(s: String) {}
