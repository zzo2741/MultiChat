package chat6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class MultiServer
{

	static ServerSocket serverSocket = null;
	static Socket socket = null;
	// 클라이언트 정보 저장을 위한 Map컬렉션 정의
	HashMap<String, PrintWriter> clientMap;

	// 생성자
	public MultiServer()
	{
		// 클라이언트의 이름과 출력스트림을 저장할 HashMap생성
		clientMap = new HashMap<String, PrintWriter>();

		// HashMap동기화 설정, 스레드가 사용자 정보에 동시에 접근하는 것을 차단한다.
		Collections.synchronizedMap(clientMap);
	}

	// 서버 초기화
	public void init()
	{

		try
		{
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");

			while (true)
			{
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress() + " : " + socket);
				/*
				 * 클라이언트의 메세지를 모든 클라이언트에게 전달하기 위한 스레드 생성 및 start.
				 */
				Thread mst = new MultiServerT(socket);
				mst.start();
			}

		} catch (Exception e)
		{
			System.out.println("예외1 : " + e);
		} finally
		{
			try
			{
				serverSocket.close();
			} catch (Exception e2)
			{
				System.out.println("예외2 : " + e2);
			}
		}
	}

	// 메인메소드 : Server객체를 생성한 후 초기화한다.
	public static void main(String[] args)
	{
		MultiServer ms = new MultiServer();
		ms.init();
	}

	// 접속된 모든 클라이언트에게 메세지를 전달하는 역할의 메소드
	public void sendAllMsg(String name, String msg)
	{
		// Map에 저장된 객체의 키값(이름)을 먼저 얻어온다.
		Iterator<String> it = clientMap.keySet().iterator();

		// 저장된 객체(클라이언트)의 갯수만큼 반복한다.
		while (it.hasNext())
		{
			try
			{
				// 각 클라이언트의 PrintWriter객체를 얻어온다.
				PrintWriter it_out = (PrintWriter) clientMap.get(it.next());

				// 클라이언트에게 메세지를 전달
				/*
				 * 매개변수 name이 있는 경우에는 이름 + 메세지 없는 경우에는 메세지만 클라이언트로 전달한다.
				 */
				if (name.equals(""))
				{
					it_out.println(msg);
				} else
				{
					it_out.println("[" + name + "] : " + msg);
				}
			} catch (Exception e)
			{
				System.out.println("예외 : " + e);
			}
		}

	}

	class MultiServerT extends Thread
	{
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;

		// 생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket)
		{
			this.socket = socket;
			try
			{
				out = new PrintWriter(this.socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			} catch (Exception e)
			{
				System.out.println("예외 : " + e);
			}
		}

		@Override
		public void run()
		{
			// 클라이언트로부터 전송된 "대화명"을 저장할 변수
			String name = "";

			// 메세지 저장용 변수
			String s = "";

			try
			{
				// 클라이언트의 이름을 읽어와서 저장
				name = in.readLine();

				// 접속한 클라이언트에게 새로운 사용자의 입장을 알림,
				// 접속자를 제외한 나머지 클라이언트만 입장 메세지를 받는다.
				sendAllMsg("", name + "님이 입장하셨습니다.");

				// 현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);

				// HashMap에 저장된 객체의 수로 접속자 수를 파악할 수 있다.
				System.out.println(name + " 접속");
				System.out.println("현재 접속자 수는 " + clientMap.size() + "명 입니다.");

				// 입력한 메세지는 모든 클라이언트에게 Echo된다.
				while (in != null)
				{
					s = in.readLine();
					if (s == null)
						break;

					System.out.println(name + " >> " + s);
					sendAllMsg(name, s);
				}
			} catch (Exception e)
			{
				System.out.println("예외 : " + e);

			} finally
			{
				/*
				 * 클라이언트가 접속을 종료하면 예외가 발생하게 되어 finally로 넘어오게 된다.
				 * 이때 "대화명"을 통해 remove()시켜준다.
				 */
				clientMap.remove(name);
				sendAllMsg("", name + "님이 퇴장하셨습니다.");
				System.out.println(name + " [" + Thread.currentThread().getName() + "] 퇴장");
				System.out.println("현재 접속자 수는 " + clientMap.size() + "명 입니다.");
				try
				{
					in.close();
					out.close();
					socket.close();
				} catch (Exception e2)
				{
					e2.printStackTrace();
				}
			}
		}

	}
}
