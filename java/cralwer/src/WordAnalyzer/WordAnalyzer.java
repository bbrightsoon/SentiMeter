package WordAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

import kr.co.shineware.nlp.komoran.core.analyzer.Komoran;
import kr.co.shineware.util.common.model.Pair;

import java.io.File;
import java.lang.Math;

public class WordAnalyzer {
   static Komoran komoran;

   static MongoClient mongoClient = new MongoClient("localhost", 27017); // 몽고디비
                                                         // 연결
   static DB db = mongoClient.getDB("Senti_Meter"); // db 변수에 testDB DB에 연결
   static DBCollection wordCollection = db.getCollection("SNS_Data"); // coll
                                                      // 변수에
                                                      // testCol
                                                      // 컬렉션에
                                                      // 연결
   static DBCollection collectionName = db.getCollection("SNS_Data");
   static String tempSL;
   static String tempSN;
   static String temp;
   static String tempNNGNNP;
   static boolean flagSL = false;
   static boolean flagSN = false;
   static boolean flagNNGNNP = false;
   static long statusId;

   public WordAnalyzer() {
		String path = new File("").getAbsolutePath();
		komoran = new Komoran(path + "/lib/models-full");
		komoran.setUserDic(path + "/voca.txt");
	}
   
   public void setStatusId(long input) {
      this.statusId = input;
   }

   public static boolean combineNN(List listA, List listB, List listC, Pair<String, String> pair, String inputText,
         List<IsNegative> isList, List<IsNegative> allList, int incnt) { // 명사와
                                                         // 명사를
                                                         // 합성

      int i = 1; // 여러번 수행하기 위한 변수

      if (listB.get(listB.size() - i).equals("NNG") || listB.get(listB.size() - i).equals("NNP")) {
         while (i <= 5) { // 최대 4개의 단어를 합성

            if (listB.get(listB.size() - i).equals("NNG") || listB.get(listB.size() - i).equals("NNP")) { // 지금
                                                                                    // 형태소가
                                                                                    // 명사이고
               temp = (String) listA.get(listA.size() - 1);
               if (listB.size() > i) {

                  if (listB.get(listB.size() - i - 1).equals("NNG")
                        || listB.get(listB.size() - i - 1).equals("NNP")) { // 이전
                                                               // 형태소가
                                                               // 명사라면

                     if (inputText
                           .charAt(inputText.indexOf(listA.get(listA.size() - i - 1).toString())
                                 + listA.get(listA.size() - i - 1).toString().length()) != ' '
                           && inputText.charAt(inputText.indexOf(listA.get(listA.size() - i - 1).toString())
                                 + listA.get(listA.size() - i - 1).toString().length()) != '\n') {// 그리고
                                                                                    // 이전형태소와
                                                                                    // 지금
                                                                                    // 형태소
                                                                                    // 사이에
                                                                                    // 빈칸
                                                                                    // 또는
                                                                                    // 줄바꿈
                                                                                    // 이
                                                                                    // 없다면

                        i++;
                        // System.out.println("지금 문장은 "+ pair+ "i가 ++됐음
                        // i는 "+i);

                     } else
                        break; // 빈칸있으면 break
                  } else
                     break; // 이전 형태소가 명사가 아니면
               } else
                  break; // 사이즈 만족안하면
            }
         }
         if (i >= 2) { // 합성조건이 만족된다면

            // System.out.println("i는 "+i);
            // if (i==3)temp = listA.get(listA.size()-2).toString()+
            // listA.get(listA.size()-1).toString(); //두단어 합성
            // else if(i==5)temp =listA.get(listA.size()-4).toString()+
            // listA.get(listA.size()-2).toString()+
            // listA.get(listA.size()-1).toString(); //세단어 합성
            // else if (i==6)temp =
            // listA.get(listA.size()-6).toString()+listA.get(listA.size()-4).toString()+listA.get(listA.size()-2).toString()+listA.get(listA.size()-1).toString();
            // //네단어 합성
            for (int j = 1; j < i; j++) {
               temp = listA.get(listA.size() - j - 1).toString() + temp;
            }
//            System.out.println("NN+ NN의 합쳐진 값은 " + temp); // 명사가 합쳐졌다면
            listC.add(temp);
            listC.remove(listC.size() - 3);
            listC.remove(listC.size() - 2); // 명사의 중복저장을 막는 것

            setisList(isList, allList, incnt, temp);

            isList.get(isList.size() - 3).isSub = true;
            allList.get(allList.size() - 3).isSub = true;
            isList.get(isList.size() - 2).isSub = true;
            allList.get(allList.size() - 2).isSub = true;

            return true;

         }
      }
      return false;
      // 지금 이 경우 아이서울유 일때 아이서울도 복합명사 저장하고 아이서울유도 저장하고 있는 문제발생
   }

   public static boolean combineNX(List listA, List listB, List listC, Pair<String, String> pair, String inputText,
         List<IsNegative> isList, List<IsNegative> allList, int incnt) {

      if (listB.get(listB.size() - 1).toString().equals("XSN")) { // 지금형태소가
                                                   // X*이고 ->
                                                   // 접미사이고
         if (listB.size() > 1) {
            if (listB.get(listB.size() - 2).toString().equals("NNG")
                  || listB.get(listB.size() - 2).toString().equals("NNP")) { // 이전
                                                               // 형태소가
                                                               // 명사일때

               if (inputText
                     .charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != ' '
                     && inputText.charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != '\n') { // 빈칸이
                                                                           // 없다면

                  temp = listA.get(listA.size() - 2).toString() + listA.get(listA.size() - 1).toString(); // 둘의
                                                                                    // 합을
                                                                                    // temp에
                                                                                    // 저장
//                  System.out.println("명사+접미사 합쳐서 " + temp);
                  listC.add(temp);
                  listC.remove(listC.size() - 2); // 명사의 중복저장을 막기위한 제거

                  isList.get(isList.size() - 1).isSub = true;
                  allList.get(allList.size() - 2).isSub = true;

                  setisList(isList, allList, incnt, temp);

                  return true;
               }
            }
         }
      }
      return false;
   }

   public static boolean combineSNWithNN(List listA, List listB, List listC, Pair<String, String> pair,
         String inputText, List<IsNegative> isList, List<IsNegative> allList, int incnt) {

      if (listB.get(listB.size() - 1).toString().equals("NNG")
            || listB.get(listB.size() - 1).toString().equals("NNP")) { // 지금형태소가
                                                         // 명사이고
         if (listB.size() > 1) {
            if (listB.get(listB.size() - 2).toString().equals("SN")) { // 이전
                                                         // 형태소가
                                                         // 숫자일때
                                                         // (SL과
                                                         // SE등
                                                         // 다른거
                                                         // 추가안했음)

               if (inputText
                     .charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != ' '
                     && inputText.charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != '\n') { // 빈칸이
                                                                           // 없다면

                  temp = listA.get(listA.size() - 2).toString() + listA.get(listA.size() - 1).toString(); // 둘의
                                                                                    // 합을
                                                                                    // temp에
                                                                                    // 저장
//                  System.out.println("숫자+명사 합쳐서 " + temp);
                  listC.add(temp);

                  isList.get(isList.size() - 1).isSub = true;
                  allList.get(allList.size() - 1).isSub = true;

                  setisList(isList, allList, incnt, temp);

                  return true;
               }
            }
         }
      }
      return false;
   }

   public static boolean combineNNWithS(List listA, List listB, List listC, Pair<String, String> pair,
         String inputText, List<IsNegative> isList, List<IsNegative> allList, int incnt) { // NN과
                                                                        // SN또는
                                                                        // SL이
                                                                        // 최대
                                                                        // 3개까지
                                                                        // 합치기
      if (listB.size() >= 2) {
         if (listB.get(listB.size() - 1).toString().equals("SN")
               || listB.get(listB.size() - 1).toString().equals("SL")) { // 지금형태소가
                                                            // SN또는
                                                            // SL이고
            if (listB.get(listB.size() - 2).toString().equals("NNG")
                  || listB.get(listB.size() - 2).toString().equals("NNP")) { // 이전형태소가
                                                               // NN일때
               if (inputText
                     .charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != ' '
                     && inputText.charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != '\n') { // 빈칸이
                                                                           // 없다면

                  temp = listA.get(listA.size() - 2).toString() + listA.get(listA.size() - 1).toString(); // 둘의
                                                                                    // 합을
                                                                                    // temp에
                                                                                    // 저장
//                  System.out.println("NNG(NNP)+SN(SL) 합쳐서 " + temp);
                  listC.add(temp);

                  isList.get(isList.size() - 1).isSub = true;
                  allList.get(allList.size() - 2).isSub = true;

                  setisList(isList, allList, incnt, temp);

                  return true;
               }
            }

            else if (listB.get(listB.size() - 2).toString().equals("SN")
                  || listB.get(listB.size() - 2).toString().equals("SL")) { // 이전
                                                               // 형태소가
                                                               // SN또는
                                                               // SL일경우
               if (listB.size() >= 3) {
                  if (listB.get(listB.size() - 3).toString().equals("NNG")
                        || listB.get(listB.size() - 3).toString().equals("NNP")) {// 전전
                                                                     // 형태소가
                                                                     // NN일때
                     if (inputText
                           .charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                                 + listA.get(listA.size() - 2).toString().length()) != ' '
                           && inputText.charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                                 + listA.get(listA.size() - 2).toString().length()) != '\n') { // 빈칸이
                                                                                 // 없다면

                        temp = listA.get(listA.size() - 3).toString() + listA.get(listA.size() - 2).toString()
                              + listA.get(listA.size() - 1).toString(); // 셋의
                                                               // 합을
                                                               // temp에
                                                               // 저장
//                        System.out.println("명사+SN(SL) +SL(SN)합쳐서 " + temp);
                        listC.remove(listC.size() - 1);
                        listC.add(temp);

                        isList.get(isList.size() - 1).isSub = true;
                        allList.get(allList.size() - 3).isSub = true;

                        setisList(isList, allList, incnt, temp);

                        return true;
                     }
                  }
               }
            }
         }
      }
      return false;
   }

   public static boolean combineNNGWithXSV(List listA, List listB, List listC, Pair<String, String> pair,
         String inputText, List<IsNegative> inList, List<IsNegative> allList, int incnt) {

      if (listB.get(listB.size() - 1).toString().equals("XSV")) { // 기분좋다 경우
                                                   // 좋다로 처리
                                                   // (기분좋다,
                                                   // 개좋아 등 결과가
                                                   // 중립으로 나와서)
         if (listB.size() > 1) {
            if (listB.get(listB.size() - 2).toString().equals("NNG")) { // 이전
                                                         // 형태소가
                                                         // 명사일때

               if (inputText
                     .charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != ' '
                     && inputText.charAt(inputText.indexOf(listA.get(listA.size() - 2).toString())
                           + listA.get(listA.size() - 2).toString().length()) != '\n') { // 빈칸이
                                                                           // 없다면

                  temp = listA.get(listA.size() - 2).toString() + listA.get(listA.size() - 1).toString(); // 둘의
                                                                                    // 합을
                                                                                    // temp에
                                                                                    // 저장
//                  System.out.println("명사+접미사(XSV) 합쳐서 (VV인) " + temp);
                  listC.add(temp + "다");

                  IsNegative in = new IsNegative();
                  in.isNegative = false;
                  in.text = temp + "다";
                  in.type = "VV";
                  in.order = incnt;
                  inList.add(in);

                  IsNegative all = new IsNegative();
                  all.isNegative = false;
                  all.text = temp + "다";
                  all.type = "VV";
                  all.order = incnt;
                  allList.add(all);
                  return true;
               }
            }
         }
      }
      return false;
   }

   public static void setisList(List<IsNegative> isList, List<IsNegative> allList, int incnt, String temp) { // is리스트와
                                                                                    // all리스트를
                                                                                    // 초기화하는
                                                                                    // 함수
      IsNegative is = new IsNegative();
      is.isNegative = false;
      is.text = temp;
      is.type = "NN";
      is.order = incnt;
      is.isSub = false;
      isList.add(is);

      IsNegative all = new IsNegative();
      all.isNegative = false;
      all.text = temp;
      all.type = "NN";
      all.order = incnt;
      all.isSub = false;
      allList.add(all);
   }

   public static double calculateTF(List listA, int a) {

      int cnt = 0;
      for (int i = 0; i < listA.size(); i++) {
         if (listA.get(i).toString().equals(listA.get(a).toString())) {
            cnt++;
         }
      }
      double d = Double.parseDouble(String.format("%.3f", (double) cnt / listA.size()));

      return d;
   }

   public static void checkNegative(List listA, List listB, List<IsNegative> allList, List<IsNegative> inList) {
      int k = -1;
      for (int i = 0; i < allList.size(); i++) {
         if (allList.get(i).type.equals("VV") || allList.get(i).type.equals("VA")) { // 형태소가
                                                                  // 동사||형용사이고
            if (i > 1) {
               if (allList.get(i - 1).text.equals("안") || allList.get(i - 1).text.equals("못")) {// 이전
                  // 단어가
                  // 안
                  // 또는
                  // 못일때
               //System.out.println("이 동사 " + allList.get(i).text + " 는 Negative 요소를 갖고 있음");
               allList.get(i).isNegative = true;
               
               for (int j = 0; j < inList.size(); j++) {
               if (allList.get(i).order == inList.get(j).order) { // inlist와
               // alllist의
               // order가
               // 같은
               // 값을
               // 찾아서
               // inlist의
               // 그
               // 인덱스
               // 값을
               // 갖는
               // inlist의
               // negative를
               // 트루로
               k = j;
               break;
               }
               }
               inList.get(k).isNegative = true;
               }
            }
            

            if (i < allList.size() - 2) {
               if (allList.get(i + 1).text.equals("않") || allList.get(i + 1).text.equals("못")
                     || allList.get(i + 1).text.equals("뻔") || allList.get(i + 1).text.equals("지만")
                     || allList.get(i + 2).text.equals("않") || allList.get(i + 2).text.equals("못")
                     || allList.get(i + 2).text.equals("뻔") || allList.get(i + 2).text.equals("지만")
                     || allList.get(i + 1).text.equals("말") || allList.get(i + 2).text.equals("말")
                     || allList.get(i + 1).text.equals("없") || allList.get(i + 2).text.equals("없")
                     || allList.get(i + 1).text.equals("못") || allList.get(i + 2).text.equals("못")) { // 다음
                                                                                    // 두단어가
                                                                                    // 않
                                                                                    // 또는
                                                                                    // 못을
                                                                                    // 갖고있을
                                                                                    // 시
//                  System.out.println("이 동사 " + allList.get(i).text + " 는 Negative 요소를 갖고 있음");
                  allList.get(i).isNegative = true;

                  for (int j = 0; j < inList.size(); j++) {
                     if (allList.get(i).order == inList.get(j).order) {
                        k = j;
                        break;
                     }
                  }
                  inList.get(k).isNegative = true;
               }
            }
            
         }
      }
   }

   public static void analyzer(String inputText) {
      
      System.out.println("\n-----------------------새로운 글 시작---------------------");
      System.out.println(inputText);

      inputText = inputText.replace('\'', ' ');
      
      // VA형용사 VV동사
      int i = 0; // 해쉬 위한 변수
      int INCount = 0;
      boolean kk = false;
      boolean kkk = false;
      boolean hh = false;
      boolean hhh = false;
      boolean uu = false;
      boolean uuu = false;
      String inputTextTemp = inputText.replace("ㅜ", "ㅠ");
      
      if (inputTextTemp.contains("ㅋㅋㅋㅋㅋ")) {
         kkk = true;
      } else if (inputTextTemp.contains("ㅋㅋ")) {
         kk = true;
      }
      
      if (inputTextTemp.contains("ㅎㅎㅎㅎㅎ")) {
         hhh = true;
      } else if (inputTextTemp.contains("ㅎㅎ")) {
         hh = true;
      }
      
      if (inputTextTemp.contains("ㅠㅠㅠㅠㅠ")) {
         uuu = true;
      } else if (inputTextTemp.contains("ㅠㅠ")) {
         uu = true;
      }

      List listA = new ArrayList();
      List listB = new ArrayList();
      List finalWords = new ArrayList(); // 디비에 저장시킬 명사들 리스트
      List infinitive = new ArrayList(); // 동사원형 리스트
      List posNeg = new ArrayList(); // 긍정부정 리스트
      List<IsNegative> inList = new ArrayList(); // 부정첵 리스트
      List<IsNegative> allList = new ArrayList(); // 모든것 담는 리스트
      List<IsNegative> isList = new ArrayList(); // sub인지 담는 리스트

      try {
         komoran.setUserDic("voca.txt");
      } catch (Exception e) {
         System.out.println((e.getStackTrace()));
      }
      List<List<Pair<String, String>>> result = komoran.analyze(inputText); // 이곳에
                                                            // 분석하고싶은
                                                            // 문장넣기

      for (List<Pair<String, String>> eojeolResult : result) {

         for (Pair<String, String> wordMorph : eojeolResult) {
            {
               // if(wordMorph.getSecond().contains("NNG")||wordMorph.getSecond().contains("NNP"))
//               System.out.println(wordMorph);

               if (wordMorph.getSecond().contains("NNG") || wordMorph.getSecond().contains("NNP")) {
                  finalWords.add(wordMorph.getFirst()); // 명사면 추가

                  i++;
               } else if (wordMorph.getSecond().contains("VV") || wordMorph.getSecond().contains("VA")) {
                  infinitive.add(wordMorph.getFirst() + "다"); // 형용사또는 동사를
                                                   // 저장

                  i++;
               } else if (wordMorph.getSecond().contains("VCP") || wordMorph.getSecond().contains("VCN")) {
                  posNeg.add(wordMorph.getFirst()); // 긍 부정을 저장

                  i++;
               }

               listA.add(wordMorph.getFirst());
               listB.add(wordMorph.getSecond());

               IsNegative in = new IsNegative(); // 부정첵위한 클래스 선언
               IsNegative all = new IsNegative(); // 모든 정보를 담는 클래스
               IsNegative is = new IsNegative(); // sub를 위한 담는 클래스

               all.text = wordMorph.getFirst();
               all.order = INCount;
               all.isNegative = false;
               all.type = wordMorph.getSecond();
               allList.add(all);

               if (wordMorph.getSecond().toString().equals("VV")
                     || wordMorph.getSecond().toString().equals("VA")) { // 동사나
                                                            // 형용사면
                                                            // inList에
                                                            // 추가한다.
                  in.text = wordMorph.getFirst() + "다";
                  in.order = INCount;
                  in.isNegative = false;
                  in.type = wordMorph.getSecond();
                  inList.add(in);
               } else if (wordMorph.getSecond().toString().equals("NNG")
                     || wordMorph.getSecond().toString().equals("NNP")) { // 명사일때
                                                               // isList에
                                                               // 추가

                  is.order = INCount;
                  is.isNegative = false;
                  is.isSub = false;
                  is.text = wordMorph.getFirst();
                  is.type = wordMorph.getSecond();
                  isList.add(is);
               }

               INCount++;

               if (combineNN(listA, listB, finalWords, wordMorph, inputText, isList, allList, INCount))
                  INCount++;
               if (combineNX(listA, listB, finalWords, wordMorph, inputText, isList, allList, INCount))
                  INCount++;
               if (combineSNWithNN(listA, listB, finalWords, wordMorph, inputText, isList, allList, INCount))
                  INCount++;
               if (combineNNWithS(listA, listB, finalWords, wordMorph, inputText, isList, allList, INCount))
                  INCount++;
               if (combineNNGWithXSV(listA, listB, infinitive, wordMorph, inputText, inList, allList, INCount))
                  INCount++;

            }
         }
      }

      /*
       * String aaa="dgh"; String json = "{ 'name' : 'lokesh' , " +
       * "'website' : 'howtodoinjava.com' , " +
       * "'address' : [{ 'addressLine1' : 'Some address'} , " +
       * "{'addressLine2' : '" +aaa +"' }, " +
       * "{'addressLine3' : 'New Delhi, India'}]" + "}";
       * 
       * String aa="0.5"; String json1 = "{ 'name' : 'lokesh' , " +
       * "'website' : 'howtodoinjava.com' , " +
       * "'address' : [{ 'addressLine1' : 'Some address'} , " +
       * "{'addressLine2' : " + aa+" }, " +
       * "{'addressLine3' : 'New Delhi, India'}]" + "}";
       */

      checkNegative(listA, listB, allList, inList);

      String jsonNoun = "";
      String jsonVerb = "";
      String jsonEmoticon = "";
      
      int isListSize = isList.size();
      
      for (int k = 0; k < isListSize; k++) {
         if (k == 0) {
            jsonNoun += "[";
         }
         jsonNoun += "{'word' : '" + isList.get(k).text + "'," + "'isSub' : '" + Boolean.toString(isList.get(k).isSub) + "'}";
         
         if (k == (isListSize - 1)) {
            jsonNoun = jsonNoun.substring(0, jsonNoun.length());
            jsonNoun += "]";
         } else {
            jsonNoun += ",";
         }
      }
      System.out.println("jsonNoun: " + jsonNoun);

      int inListSize = inList.size();
      for (int k = 0; k < inListSize; k++) {
         if (k == 0) {
            jsonVerb += "[";
         }
         jsonVerb += "{'word' : '" + inList.get(k).text + "'," + "'isNeg' : '"
               + Boolean.toString(inList.get(k).isNegative) + "'}";
         if (k == (inListSize - 1)) {
            jsonVerb = jsonVerb.substring(0, jsonVerb.length());
            jsonVerb += "]";
         } else {
            jsonVerb += ",";
         }
      }
      System.out.println("jsonVerb: " + jsonVerb);
      
      
      boolean flag = false;
      jsonEmoticon += "[";
      if (uuu == true) {
          if (flag == false) {
             jsonEmoticon += "{\"word\" : \"ㅠㅠㅠ\"," + "\"isSub\" : \"false\"}";
             flag = true;
          } else {
             jsonEmoticon += ",{\"word\" : \"ㅠㅠㅠ\"," + "\"isSub\" : \"false\"}";
          }
       } else if (uu == true) {
          if (flag == false) {
             jsonEmoticon += "{\"word\" : \"ㅠㅠ\"," + "\"isSub\" : \"false\"}";
             flag = true;
          } else {
             jsonEmoticon += ",{\"word\" : \"ㅠㅠ\"," + "\"isSub\" : \"false\"}";
          }
       }
      if (kkk == true) {
         if (flag == false) {
            jsonEmoticon += "{\"word\" : \"ㅋㅋㅋ\"," + "\"isSub\" : \"false\"}";
            flag = true;
         } else {
            jsonEmoticon += ",{\"word\" : \"ㅋㅋㅋ\"," + "\"isSub\" : \"false\"}";
         }
      } else if (kk == true) {
         if (flag == false) {
            jsonEmoticon += "{\"word\" : \"ㅋㅋ\"," + "\"isSub\" : \"false\"}";
            flag = true;
         } else {
            jsonEmoticon += ",{\"word\" : \"ㅋㅋ\"," + "\"isSub\" : \"false\"}";
         }
      }
      if (hhh == true) {
         if (flag == false) {
            jsonEmoticon += "{\"word\" : \"ㅎㅎㅎ\"," + "\"isSub\" : \"false\"}";
            flag = true;
         } else {
            jsonEmoticon += ",{\"word\" : \"ㅎㅎㅎ\"," + "\"isSub\" : \"false\"}";
         }
      } else if (hh == true) {
         if (flag == false) {
            jsonEmoticon += "{\"word\" : \"ㅎㅎ\"," + "\"isSub\" : \"false\"}";
            flag = true;
         } else {
            jsonEmoticon += ",{\"word\" : \"ㅎㅎ\"," + "\"isSub\" : \"false\"}";
         }
      }
      
      jsonEmoticon += "]";

      System.out.println("jsonEmoticon: " + jsonEmoticon);
      
      BasicDBObject tupleQuery = new BasicDBObject("StatusId", statusId);

      BasicDBObject nounDoc = new BasicDBObject();
      DBObject dbObjectNoun = (DBObject) JSON.parse(jsonNoun);
      nounDoc.append("$set", new BasicDBObject().append("Noun", dbObjectNoun));
      wordCollection.update(tupleQuery, nounDoc);

      BasicDBObject verbDoc = new BasicDBObject();
      DBObject dbObjectVerb = (DBObject) JSON.parse(jsonVerb);
      verbDoc.append("$set", new BasicDBObject().append("Verb", dbObjectVerb));
      wordCollection.update(tupleQuery, verbDoc);
      
      BasicDBObject emoticonDoc = new BasicDBObject();
      DBObject dbObjectEmoticon = (DBObject) JSON.parse(jsonEmoticon);
      emoticonDoc.append("$set", new BasicDBObject().append("Imoticon", dbObjectEmoticon));
      wordCollection.update(tupleQuery, emoticonDoc);

   } // analyzer함수 끝
}// 클래스 끝