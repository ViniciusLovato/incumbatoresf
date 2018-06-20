import java.sql.*
import java.time.Instant

/**
 * See also:
 *  https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html
 */

object sqlCommands {
    var getRelevantTablesNames: String =
            "SELECT TABLE_NAME \n" +
            "FROM   INFORMATION_SCHEMA.TABLES\n" +
            "WHERE TABLE_SCHEMA = 'PUBLIC'\n"
    var getTablesCount: String =
            "SELECT COUNT(TABLE_NAME) AS QTD \n" +
            "FROM   INFORMATION_SCHEMA.TABLES\n" +
            "WHERE TABLE_SCHEMA = 'PUBLIC'\n"
}

class DbSimpleInstance {
    var connectionUrl: String = "jdbc:hsqldb:hsql://localhost:9001/teste1"
        get(){
            /**
             * See
             * https://medium.com/@agrawalsuneet/backing-field-in-kotlin-bd9c2d5b6da5
             */
            var ret: String = ""
            if (this.isConnectionReadonly)
                ret += "readonly=true"
            return field + ";" + ret
        }
    var connectionUser: String = "SA"
    var connectionPassword: String = ""
//    var connectionTimestamp = Instant.now()
    lateinit var connection: Connection
    lateinit var statement: Statement

    /**
     * See
     *  http://hsqldb.org/doc/guide/dbproperties-chapt.html#N15CD1
     */
    var isConnectionReadonly: Boolean
        get() = false
        set(value:Boolean){
            this.isConnectionReadonly = value
        }

    constructor()
    {
        try {
            Class.forName("org.hsqldb.jdbcDriver")
        } catch (e: Exception) {
            println("Can't load DB: ${e.message}")
            return
        }
    }

    fun connect(): Boolean
    {
        try {
            this.connection = DriverManager.getConnection(this.connectionUrl, this.connectionUser, this.connectionPassword)
        } catch (e: Exception) {
            println("Can't create DB: ${e.message}")
            return false
        }
        this.statement = connection.createStatement()
        return true
    }

    fun connectAsReadonly(){
        this.isConnectionReadonly = true
        this.connect()
    }

    private fun executeSql(sql:String): ResultSet
    {
        var isConnOk: Boolean = true
        /**
         * For `Unresolved reference: isInitialized` error, see:
         *  https://stackoverflow.com/a/47105843/7362660
         */
        if (!this::connection.isInitialized || this.connection == null){
            println("WARN: trying to establish connection (lately) to:\t'" + this.connectionUrl + "'")
            isConnOk = this.connect()
            if (!isConnOk){
                println("failed to connect")
            }
        }

        return this.statement.executeQuery(sql)
    }

    fun testConnection(){
        val rs = this.executeSql("VALUES (current_timestamp)")
        while (rs.next()) {
            println(rs.getString("C1"))
        }
    }
}

fun syncDatabases(srcDb: DbSimpleInstance, dstDb: DbSimpleInstance){
    ///TODO: implement this, currently is mockup
//    val srcDbMd: DatabaseMetaData = srcDb.connection.getMetaData()
//    val dstDbMd: DatabaseMetaData = dstDb.connection.getMetaData()
//    val srcRsTables: ResultSet = srcDbMd.getTables(null, null, null, null)
//    val dstRsTables: ResultSet = srcDbMd.getTables(null, null, null, null)

    val srcRsCount = srcDb.statement.executeQuery(sqlCommands.getTablesCount)
    if (srcRsCount.next()){
        val srcTablesCount = srcRsCount.getInt("QTD")
        print("src database tables count:\t'$srcTablesCount'")
    }else{
        print("some problem when iterating in resultset")
    }

}

fun main(args: Array<String>)
{
    //print("hello world")

//    var dbRead: String = readInput("readonly connection url", "jdbc:hsqldb:hsql://localhost:9001/teste1")
//    print(dbRead)
//    var dbUser: String = readInput("readonly connection user", "SA")
//    print(dbUser)
//    var dbPassword: String = readInput("readonly connection password", "")
//    print(dbPassword)

//    var dbRead: String = "jdbc:hsqldb:hsql://localhost:9001/teste1"
//    var dbUser: String = "SA"
//    var dbPassword: String = ""
//
//    hsqldb(dbRead, dbUser, dbPassword)

    var a = DbSimpleInstance()
    a.connect()
//    a.testConnection()


    var dest = DbSimpleInstance()
    dest.connectionUrl = "jdbc:hsqldb:hsql://localhost:49001/dest"
    dest.connect()
//    dest.testConnection()


    syncDatabases(a, dest)


}

fun readInput(consoleMessage: String, defaultValue: String = "", prefix: String = "→ "): String
{
    var ret = defaultValue
    println("\n$prefix $consoleMessage [$defaultValue]:")
    var aux = readLine()
    if (aux != null && aux != " "){
        ret = aux.toString()
    }
    return ret
}

fun hsqldb(dbConnStr: String, dbUser: String, dbPassword: String)
{
    println("HSQLDB")
    try {
        Class.forName("org.hsqldb.jdbcDriver")
    } catch (e: Exception) {
        println("Can't load DB: ${e.message}")
        return
    }

    try {
        val connection: Connection = DriverManager.getConnection(dbConnStr, dbUser, dbPassword)
        val statement = connection.createStatement()

        val dbm: DatabaseMetaData = connection.getMetaData()
//        val rsEmployee: ResultSet = dbm.getTables( /*catalog*/ null, /*schema*/ null, "employee", null)
//        val rsUsuarios: ResultSet = dbm.getTables( /*catalog*/ null, /*schema*/ null, "USUARIOS", null)
//        println(rsEmployee.next())
//        println(rsUsuarios.next())

        val relevantTables = getRelevantTablesNames(statement)
        for (currTabName:String in relevantTables){
            val rsTabPresence:ResultSet = dbm.getTables(null, null, currTabName, null)
            val isCurrTabPresent:Boolean = rsTabPresence.next()

            println("${currTabName} -> ${isCurrTabPresent}")
            if (isCurrTabPresent){
                copyAllTableData(currTabName)
            }else{
                //TODO
                println("create table '${currTabName}'")
            }
        }


//        statement.executeUpdate("drop table if exists journal");
//        statement.executeUpdate("CREATE TABLE journal (id IDENTITY, name VARCHAR(4096))")
//        for (i in 0..10) {
//            statement.executeUpdate("INSERT INTO journal (name) VALUES('woohoo')")
//        }
//        val rs = statement.executeQuery("select * from journal")
//        while(rs.next()) {
//            println("id=${rs.getInt("id")} name = ${rs.getString("name")}")
//        }
//        statement.execute("SHUTDOWN")

        connection.close()
    } catch (e: Exception) {
        println("Can't create DB: ${e.message}")
        return
    }
}

fun getRelevantTablesNames(statement: Statement): MutableList<String>
{
    val rs = statement.executeQuery(sqlCommands.getRelevantTablesNames)
    var ret: MutableList<String> = mutableListOf<String>()

    //ret = rs.getArray("TABLE_NAME").array as MutableList<String>
    ///Can't create DB: invalid cursor state: identifier cursor not positioned on row in UPDATE, DELETE, SET, or GET statement: ; ResultSet is positioned before first row

    while(rs.next()) {
//        println("TABLE_NAME = ${rs.getString("TABLE_NAME")}")
        val auxTabName = rs.getString("TABLE_NAME")
        ret.add(ret.size, auxTabName)
    }
    return ret
}

fun copyAllTableData(currTabName:String)
{

}