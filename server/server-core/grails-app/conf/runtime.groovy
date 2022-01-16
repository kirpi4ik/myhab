grails.gorm.default.mapping = {
    id(generator: 'native')
    '*'(cascadeValidate: 'owned')
}