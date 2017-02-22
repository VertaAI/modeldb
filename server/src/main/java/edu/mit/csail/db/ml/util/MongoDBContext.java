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
    
    public static void main( String args[] ) {
    	try {
        	
        	// MongoClient mongoClient = MongoDBContext.create("testuser", "testpwd", "mongodb://localhost:27017", "users");
        	MongoClient mongoClient = new MongoClient();

        	// Now connect to the databases
        	DB mongoDB = mongoClient.getDB("modeldb_mongodb");
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