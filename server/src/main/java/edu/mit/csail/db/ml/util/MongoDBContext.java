package edu.mit.csail.db.ml.util;
import edu.mit.csail.db.ml.conf.ModelDbConfig;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * This class contains logic for connecting to the MongoDB database.
 */
public class MongoDBContext {
    
    /**
     * Create a MongoDB database context that reflects a connection to a database.
     * @param username - The username to connect to the database.
     * @param password - The password to connect to the database.
     * @param jdbcUrl - The JDBC URL of the database.
     * @param userDB - The database where the user is defined.
     * @return The database context.
     * @throws IllegalArgumentException - Thrown if the dbType is unsupported.
     */
	public static MongoClient create(String username, String password, String jdbcUrl, String userDB) throws IllegalArgumentException {
		try {
			// To connect to mongodb server
			MongoCredential credential = MongoCredential.createCredential(username, userDB, password.toCharArray());
			
			// commented out since we don't yet a have db filled with user credential so we cannot authenticate ourselves
//			MongoClient mongoClient = new MongoClient(new ServerAddress(jdbUrl), Arrays.asList(credential));
			MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017));
			
			System.out.println("Connect to database successfully");
			return mongoClient;
		} catch (Exception e) {                                                                                                                                                                                                                                                                                                   
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			throw new IllegalArgumentException("Cannot connect to the MongoDB server at" + jdbcUrl);
		}
	}
    
    
    public static void main( String args[] ) {
    	// an example of how the method above will be used
        try {
        	
        	MongoClient mongoClient = MongoDBContext.create("testuser", "testpwd", "mongodb://localhost:27017", "users");
        	
        	// Now connect to the databases
        	DB mongoDB = mongoClient.getDB("testDB");
            DBCollection testCollection = mongoDB.getCollection("testCollection");
            System.out.println("Collection testCollection selected successfully");
            
            // create a db object and insert it to the database
            BasicDBObject doc = new BasicDBObject("title", "MongoDB").append("description", "database")
                    .append("size", 100).append("url", "test_url");
            testCollection.insert(doc);
            System.out.println("Document inserted successfully");
            
            // find the document that just got inserted
            DBCursor cursor = testCollection.find();
            int i = 1;
            while (cursor.hasNext()) { 
               System.out.println("Inserted Document: "+ i); 
               System.out.println(cursor.next()); 
               i++;
            }
            
            // close the connection
            mongoClient.close();
            
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}