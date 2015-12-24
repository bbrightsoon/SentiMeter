package Crawler;
import java.util.logging.Logger;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;

import twitter4j.Status;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

public class AccessDB {
   private String text;
   private String id;
   private String dbName;
   private String collectionName;
   private String ip;
   
   private long statusId;
   private int port;

   public AccessDB() {
      Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
      mongoLogger.setLevel(Level.SEVERE);
      this.dbName = "Senti_Meter";
      this.ip = "127.0.0.1";
      this.port = 27017;
   }

   private String getTime() {
      // get insert time
      String insertTime;
      Calendar calendar = Calendar.getInstance();
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      insertTime = dateFormat.format(calendar.getTime());
   
      return insertTime;
   }

   public void setDB(String inputDB) {
      this.dbName = inputDB;
   }

   public void setCol(String inputCol) {
      this.collectionName = inputCol;
   }

   public void setIp(String inputip) {
      this.ip = inputip;
   }

   public void setPort(int inputport) {
      this.port = inputport;
   }

   public String getDB() {
      return this.dbName;
   }

   public String getCollectionName() {
      return this.collectionName;
   }

   public void printTwitterText() {
      dbName = "Senti_Meter";
      collectionName = "SNS_Data";

      String output;
      MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port));
      DB db = mongoClient.getDB(dbName);

      DBCollection col = db.getCollection(collectionName);
      DBCursor cursor = col.find();

      try {
         BufferedWriter bw = new BufferedWriter(new FileWriter(
               "C:\\Users\\Administrator\\Desktop\\text.txt"));
         while (cursor.hasNext()) {
            DBObject saveCursor = cursor.next();

            output = saveCursor.get("Name").toString();
            bw.write("닉네임 : " + output);
            bw.newLine();
            output = saveCursor.get("Time").toString();
            bw.write("시간 : " + output);
            bw.newLine();
            output = saveCursor.get("Text").toString();
            bw.write("내용 : " + output);
            bw.newLine();
            bw.newLine();
         }
         bw.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
      mongoClient.close();
   }

   public void insertTwitterText(Status status) {
      dbName = "Senti_Meter";
      collectionName = "SNS_Data";
      MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port));
      DB db = mongoClient.getDB(dbName);

      // If there is not collection, make it
      if (db.collectionExists(collectionName) == false) {
         db.createCollection(collectionName, new BasicDBObject());
      }

      // insert {Id, Name, Text, Time, ScreenName, StatusId}
      DBCollection addedCollection = db.getCollection(collectionName);
      
//      BasicDBObject uniqueQuery = new BasicDBObject("StatusId", 1);
//      addedCollection.createIndex(uniqueQuery, "StatusIdIndex", true);
      
      BasicDBObject inputTwit = new BasicDBObject();
      inputTwit.put("Id", status.getUser().getId()); // User id
      inputTwit.put("Name", status.getUser().getName()); // User nickname
      inputTwit.put("Text", status.getText());   
      inputTwit.put("Time", getTime());
      inputTwit.put("ScreenName", status.getUser().getScreenName());   // Login id
      inputTwit.put("StatusId", status.getId()); // Post number
      inputTwit.put("Noun", null);
      inputTwit.put("Verb", null);
      inputTwit.put("sentiment", null);
      inputTwit.put("flag", false);
      try {
         addedCollection.insert(inputTwit);
      } catch (com.mongodb.DuplicateKeyException e) {}
      
      statusId = status.getId();
      
      mongoClient.close();
   }

   public void addBlackListUser(Status status) {
      dbName = "Senti_Meter";
      collectionName = "Black_List";

      MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port));
      DB db = mongoClient.getDB(dbName);

      if (db.collectionExists(collectionName) == false) {
         db.createCollection(collectionName, new BasicDBObject());
      }

      DBCollection addedCollection = db.getCollection(collectionName);

      if (getBlackListCount(status) == 0) {
         BasicDBObject inputUser = new BasicDBObject();
         inputUser.put("Screen_Name", status.getUser().getScreenName());   // Login id
         inputUser.put("Name", status.getUser().getName());            // Nickname
         inputUser.put("Time", getTime());                        // Time
         inputUser.put("Text", status.getText());                     
         inputUser.put("Count", 1);                              
         inputUser.put("StatusId", status.getId());
         addedCollection.insert(inputUser);
      } else {
         BasicDBObject findQuery = new BasicDBObject("Screen_Name", status
               .getUser().getScreenName());
         BasicDBObject newInput = new BasicDBObject("Screen_Name", status.getUser()
               .getScreenName());
         newInput.put("Name", status.getUser().getName());
         newInput.put("Time", getTime());
         newInput.put("Text", status.getText());
         newInput.put("Count", getBlackListCount(status) + 1);
         newInput.put("StatusId", status.getId());
         BasicDBObject updateObject = new BasicDBObject("$set", newInput);
         addedCollection.update(findQuery, updateObject);
      }
      mongoClient.close();
   }

   public int getBlackListCount(Status status) {
      dbName = "Senti_Meter";
      collectionName = "Black_List";

      MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port));
      DB db = mongoClient.getDB(dbName);
      DBCollection col = db.getCollection(collectionName);

      BasicDBObject findQuery = new BasicDBObject("Screen_Name", status.getUser()
            .getScreenName());
      DBObject result = col.findOne(findQuery);
      
      if (result == null) {
         mongoClient.close();
         return 0;
      } else {
         mongoClient.close();
         return (int)result.get("Count");
      }
      // result.get() 쓰면 원하는거 얻을수 있음
      // 검색결과없으면 result - null
   }
   
   public int getSpamWordCount(Status status) {
      // DB 안쓰는 방법
      String spamWordArray[] = {"토토", "프로토", "바둑이", "카지노", "인터넷 도박", "싸롱", "바카라", "살롱", "배팅", "핸플", "건마",
            "캔디넷", "춘자넷", "미소넷", "레드트리", "야밤티비", "티비팡", "꿀잼넷", "런피플", "티구리", "스파크", "야한동영상", "야동",
            "soranet", "야친소", "오피뷰", "한국야동", "유휴걸", "소라넷트위터", "수유건마", "오피뷰닷컴", "토렌터스", "섹스코리아", "냄비닷컴",
            "누들티비", "딸밤닷컴", "라면닷컴", "19곰닷컴", "토랭킹", "미스폰", "일본야동", "밍키넷", "야동사이트", "유튜걸", "서양야동",
            "야동싸이트", "떡방닷컴", "앵두넷", "동네형컴퓨터", "소라넷", "강남오피", "060폰팅", "소라넷모바일", "온라인카지노", "영종도카지노", 
            "인터넷카지노주소", "인터넷카지노", "신천지카지노", "카지노게임", "월드카지노사이트", "카지노바카라", "테크노카지노", "헬로카지노", 
            "생중계카지노", "카지노싸이트", "온라인카지노사이트", "우리카지노사이트", "다모아카지노", "무료방송", "스토어팜"};
      int count = 0;

      String text = status.getText();
      String name = status.getUser().getName();
      String desc = status.getUser().getDescription();

      for (int i = 0; i < spamWordArray.length; i++) {
         if (text.contains(spamWordArray[i]) == true) {
            count++;
         }
         if (name.contains(spamWordArray[i]) == true) {
            count++;
         }
         if (desc.contains(spamWordArray[i]) == true) {
            count++;
         }
      }
      return count;
            /*      // DB 쓰는 방법
      dbName = "Senti_Meter";
      collectionName = "Spam_Word_List";
      
      MongoClient mongoClient = new MongoClient(new ServerAddress(ip, port));
      DB db = mongoClient.getDB(dbName);
      DBCollection col = db.getCollection(collectionName);
      
      DBCursor cursor = col.find();
      int count = 0;
      
      String text = status.getText();
      String name = status.getUser().getName();
      String desc = status.getUser().getDescription();
      
      while (cursor.hasNext()) {
         DBObject searchWord = cursor.next();
         if (text.contains(searchWord.get("Word").toString()) == true) {
            count++;
         }
         if (name.contains(searchWord.get("Word").toString()) == true) {
            count++;
         }
         if (desc.contains(searchWord.get("Word").toString()) == true) {
            count++;
         }
      }
      mongoClient.close();
      return count;
   */
            }

   
   public long getStatusId() {
      return statusId;   
   }
}