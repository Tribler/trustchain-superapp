package id.walt.servicematrix.exceptions

class ServiceNotFoundException(service: String) :
    ServiceMatrixException("Could not find service (to register a implementation to): \"$service\" (while parsing ServiceMatrix configuration file)! This is most likely a configuration error with the ServiceMatrix file.")
