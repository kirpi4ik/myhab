package eu.devexpert.madhouse.request

/**
 *
 */
enum Headers {
  enum REQ implements IHeader{
    AUTHORIZATION("Authorization"),

    def name

    REQ(name) {
      this.name = name
    }
  }

  enum RESP implements IHeader{
    AUTHORIZATION("Authorization"),
    REQUEST_ID("mh-reques-id")

    def name

    RESP(name) {
      this.name = name
    }
  }

}