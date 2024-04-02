package com.example.autumn.orm

import jakarta.persistence.NoResultException
import jakarta.persistence.NonUniqueResultException
import org.slf4j.LoggerFactory

interface Query<T> {
    fun query(): List<T>
    fun first(): T?
    fun unique(): T
}

/**
 * Hold criteria query information.
 *
 * @param <T> Entity type.
 */
class Criteria<T>(private val dbTemplate: DbTemplate, private val mapper: Mapper<T>) : Query<T> {
    private val logger = LoggerFactory.getLogger(this::class.java)
    val joinParams = mutableListOf<Any>()
    val whereParams = mutableListOf<Any>()
    val joinClauses = mutableListOf<String>()
    val orderBys = mutableListOf<String>()
    var distinct = false
    var whereClause = ""
    var limit = 0L
    var offset = 0L

    private fun sql(): String {
        return StringBuilder(128).also {
            it.append(if (distinct) " SELECT DISTINCT * " else " SELECT * ")
            it.append(" FROM ${mapper.tableName} ")
            if (joinClauses.isNotEmpty()) {
                it.append(joinClauses.joinToString(prefix = " JOIN ", separator = " JOIN ", postfix = " "))
            }
            if (whereClause.isNotEmpty()) {
                it.append(" WHERE $whereClause ")
            }
            if (orderBys.isNotEmpty()) {
                it.append(orderBys.joinToString(prefix = " ORDER BY ", postfix = " "))
            }
            if (limit > 0 && offset >= 0) {
                it.append(" LIMIT ? OFFSET ? ")
            }
        }.toString()
    }

    override fun query(): List<T> {
        val queryParams = (joinParams + whereParams).toMutableList()
        if (limit > 0 && offset >= 0) {
            queryParams += limit
            queryParams += offset
        }
        val querySql = sql()
        val start = System.currentTimeMillis()
        return dbTemplate.jdbcTemplate.query(querySql, mapper.resultSetExtractor, *queryParams.toTypedArray())!!.also {
            logger.trace(
                "querySql: {}, queryParams: {}, time: {}ms", querySql, queryParams, System.currentTimeMillis() - start
            )
        }
    }

    /**
     * Get first row of the query, or null if no result found.
     *
     * @return Object T or null.
     */
    override fun first(): T? {
        limit = 1
        offset = 0
        return query().firstOrNull()
    }

    /**
     * Get unique result of the query. Exception will throw if no result found or more than one results found.
     *
     * @return T modelInstance
     * @throws NoResultException        If result set is empty.
     * @throws NonUniqueResultException If more than 1 result found.
     */
    override fun unique(): T {
        limit = 2
        offset = 0
        return query().also {
            require(it.isNotEmpty()) {
                throw NoResultException("Expected unique row but nothing found.")
            }
            require(it.count() == 1) {
                throw NonUniqueResultException("Expected unique row but more than 1 rows found.")
            }
        }.first()
    }
}

class SelectFrom<T>(private val criteria: Criteria<T>, distinct: Boolean) : Query<T> {
    init {
        criteria.distinct = distinct
    }

    fun join(joinClause: String, vararg args: Any): Join<T> {
        return Join(criteria, joinClause, *args)
    }

    fun where(clause: String, vararg args: Any): Where<T> {
        return Where(criteria, clause, *args)
    }

    fun orderBy(orderBy: String, desc: Boolean = false): OrderBy<T> {
        return OrderBy(criteria, if (desc) "$orderBy DESC" else orderBy)
    }

    fun limit(limit: Long, offset: Long = 0): Limit<T> {
        return Limit(criteria, limit, offset)
    }

    override fun query(): List<T> {
        return criteria.query()
    }

    override fun first(): T? {
        return criteria.first()
    }

    override fun unique(): T {
        return criteria.unique()
    }
}

class Join<T>(private val criteria: Criteria<T>, joinClause: String, vararg args: Any) : Query<T> {
    init {
        criteria.joinClauses += joinClause
        criteria.joinParams.addAll(args)
    }

    fun join(joinClause: String, vararg args: Any): Join<T> {
        criteria.joinClauses += joinClause
        criteria.joinParams.addAll(args)
        return this
    }

    fun where(clause: String, vararg args: Any): Where<T> {
        return Where(criteria, clause, *args)
    }

    fun orderBy(orderBy: String, desc: Boolean = false): OrderBy<T> {
        return OrderBy(criteria, if (desc) "$orderBy DESC" else orderBy)
    }

    fun limit(limit: Long, offset: Long = 0): Limit<T> {
        return Limit(criteria, limit, offset)
    }

    override fun query(): List<T> {
        return criteria.query()
    }

    override fun first(): T? {
        return criteria.first()
    }

    override fun unique(): T {
        return criteria.unique()
    }
}

class Where<T>(private val criteria: Criteria<T>, clause: String, vararg params: Any) : Query<T> {
    init {
        require(clause.count { it == '?' } == params.count()) { "params counts do not match given where clause" }
        criteria.whereClause = clause
        criteria.whereParams.addAll(params)
    }

    fun orderBy(orderBy: String, desc: Boolean = false): OrderBy<T> {
        return OrderBy(criteria, if (desc) "$orderBy DESC" else orderBy)
    }

    fun limit(limit: Long, offset: Long = 0): Limit<T> {
        return Limit(criteria, offset, limit)
    }

    override fun query(): List<T> {
        return criteria.query()
    }

    override fun first(): T? {
        return criteria.first()
    }

    override fun unique(): T {
        return criteria.unique()
    }
}

class OrderBy<T>(private val criteria: Criteria<T>, orderBy: String) : Query<T> {
    init {
        criteria.orderBys += orderBy
    }

    fun orderBy(orderBy: String, desc: Boolean = false): OrderBy<T> {
        criteria.orderBys += if (desc) "$orderBy DESC" else orderBy
        return this
    }

    fun limit(limit: Long, offset: Long = 0): Limit<T> {
        return Limit(criteria, limit, offset)
    }

    override fun query(): List<T> {
        return criteria.query()
    }

    override fun first(): T? {
        return criteria.first()
    }

    override fun unique(): T {
        return criteria.unique()
    }
}

class Limit<T>(private val criteria: Criteria<T>, limit: Long, offset: Long) : Query<T> {
    init {
        require(limit > 0) { "limit must be > 0" }
        require(offset >= 0) { "offset must be >= 0" }
        criteria.limit = limit
        criteria.offset = offset
    }

    override fun query(): List<T> {
        return criteria.query()
    }

    override fun first(): T? {
        return criteria.first()
    }

    override fun unique(): T {
        return criteria.unique()
    }
}