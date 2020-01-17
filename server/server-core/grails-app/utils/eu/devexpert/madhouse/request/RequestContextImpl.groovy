package eu.devexpert.madhouse.request

/**
 *
 */
class RequestContextImpl {
  def requestId
  def remoteUser
  def remoteIp
  def vndContentType
  def vndAccept
  def authToken
  def reqMethod
  def reqUrl
  RequestContextImpl() {
    requestId = UUID.randomUUID().toString()
    remoteUser = "anonymous"
  }
}
