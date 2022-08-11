package id.walt.servicematrix.exceptions

abstract class ServiceMatrixException(msg: String) : IllegalArgumentException(
    "walt.id ServiceMatrix: $msg \n" +
            "Information about the ServiceMatrix: https://github.com/walt-id/waltid-ssikit/blob/master/README.md"
)
