package id.walt.servicematrix.exceptions

class UnimplementedServiceException(service: String?, extra: String? = null) :
    ServiceMatrixException("No implementation has been registered for service: \"$service\"${if (extra != null) " ($extra)" else ""}! This is a configuration error. ")
