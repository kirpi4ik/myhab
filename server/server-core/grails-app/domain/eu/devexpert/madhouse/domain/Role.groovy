package eu.devexpert.madhouse.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes='authority')
@ToString(includes='authority', includeNames=true, includePackage=false)
class Role implements Serializable {

    private static final long serialVersionUID = 1

    String authority

    Role(String authority) {
        this()
        this.authority = authority
    }
    static constraints = {
        authority nullable: false, blank: false, unique: true
    }

    static mapping = {
        table '`sec_roles`'
        cache true
    }
    static graphql = true
}
