package id.walt.servicematrix.exceptions

class NotValidBaseServiceException(implementationClass: String) :
    ServiceMatrixException("$implementationClass is not a valid BaseService")
