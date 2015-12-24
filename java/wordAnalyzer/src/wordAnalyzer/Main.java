package wordAnalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

	public static void main(String[] args) {
		
//		 
//		 
		WordAnalyzer wa = new WordAnalyzer();
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(7000);
			System.out.println(getTime() + "서버가 준비되었습니다.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while(true) {
			try {
				System.out.println("==================================================");
				System.out.println(getTime() + "연결요청을 기다립니다.");
				Socket socket = serverSocket.accept();
				System.out.println(getTime() + socket.getInetAddress() + "로부터 연결요청이 들어왔습니");
				
				OutputStream out = socket.getOutputStream();
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
				
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				String input = in.readLine();

				String result = wa.analyzer(input);
				writer.println(result);
				writer.flush();
				System.out.println(getTime() + " 데이터를 전송했습니다.");
				
				
				out.close();
				writer.close();
				in.close();
				socket.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	static String getTime() {
		SimpleDateFormat fromformat = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss");
		return fromformat.format(new Date());
	}
}
