package edu.mit.csail.db.ml.util;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;

import com.mongodb.ServerAddress;
import java.util.Arrays;

/**
 * This class contains logic for connecting to the MongoDB database.
 */
public class MongoDBContext {
    
    /**
     * Create a MongoDB database context that reflects a connection to a database.
     * @param username - The username to connect to the database.
     * @param password - The password to connect to the database.
     * @param jdbcUrl - The JDBC URL of the database.
     * @param dbType - The database type.
     * @return The database context.
     */
    public static void create(String username, String password, String jdbcUrl, ModelDbConfig.DatabaseType dbType) {
        // TODO: use the arguments instead of the hardcoded ones
    }
    
    
    public static void main( String args[] ) {
        try {

            // To connect to mongodb server
            MongoClient mongoClient = new MongoClient("localhost", 27017);

            // Now connect to your databases
            DB db = mongoClient.getDB("test");
            System.out.println("Connect to database successfully");

            boolean auth = db.authenticate(myUserName, myPassword);
            System.out.println("Authentication: " + auth);
            DBCollection coll = db.getCollection("mycol");
            System.out.println("Collection mycol selected successfully");

            BasicDBObject doc = new BasicDBObject("title", "MongoDB").append("description", "database")
                    .append("likes", 100).append("url", "test_url");

            coll.insert(doc);
            System.out.println("Document inserted successfully");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}