package org.myhab.domain

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.joda.time.DateTime
import org.myhab.domain.common.BaseEntity

class TimeSeriesStatistic extends BaseEntity {
    Long id
    String key
    Double value
    Double deltaDiff
    static constraints = {
        deltaDiff nullable: true
    }
    static graphql = GraphQLMapping.lazy {
        query('statLatestByKey', TimeSeriesStatistic) {
            argument('key', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    return TimeSeriesStatistic.createCriteria().list {
                        eq('key', environment.getArgument('key'))
                        order('tsCreated', 'asc')
                        maxResults(1)
                    }?.find()
                }
            })
        }
        query('statLatestByKeys', [TimeSeriesStatistic]) {
            argument('keys', [String])
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    def result = []
                    def now = DateTime.now()
                    environment.getArgument('keys').each { key ->
                        result << TimeSeriesStatistic.createCriteria().list {
                            eq('key', key)
                            and {
                                between('tsCreated', now.minusHours(3).toDate(), now.toDate())
                            }
                            order('tsCreated', 'asc')
                            maxResults(1)
                        }?.find()
                    }
                    return result
                }
            })
        }
    }
}
