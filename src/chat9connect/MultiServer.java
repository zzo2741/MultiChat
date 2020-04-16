package chat9connect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

public class MultiServer
{
	static ServerSocket serverSocket = null;
	static Socket socket = null;

	HashMap<String, PrintWriter> clientMap;
	HashMap<String, String> whisperClient;
	HashSet<String> blackList;
	DBQueryCollection db = new DBQueryCollection();

	public MultiServer()
	{
		clientMap = new HashMap<String, PrintWriter>(); // 클라이언트의 이름과 출력스트림을 저장할 HashMap생성
		whisperClient = new HashMap<>(); // 귓속말 고정한 클라이언트 저장할 HashMap생성
		blackList = new HashSet<>();
		Collections.synchronizedMap(clientMap); // HashMap동기화 설정, 스레드가 사용자 정보에 동시에 접근하는 것을 차단한다.
		Collections.synchronizedMap(whisperClient);
		db.connectDB(); // DB 접속
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

				// 클라이언트의 메세지를 모든 클라이언트에게 전달하기 위한 스레드 생성 및 start.
				Thread mst = new MultiServerT(socket);
				mst.start();

				Thread mst2 = new MultiServerT2();
				mst2.start();
			}

		} catch (Exception e)
		{
			System.out.println("MultiServer>init()>예외1 : " + e);
		} finally
		{
			try
			{
				db.disConnectCB();
				serverSocket.close();
			} catch (Exception e2)
			{
				System.out.println("MultiServer>init()>예외2 : " + e2);
			}
		}
	}

	public void serverNotice()
	{
		Scanner scanner = new Scanner(System.in);
		String serverMsg = scanner.nextLine();

		ArrayList<String> list = stringTok(serverMsg);
		if (list.size() > 1)
		{
			if (list.get(0).equals("/notice"))
			{
				Iterator<String> it = clientMap.keySet().iterator();

				while (it.hasNext())
				{
					try
					{
						PrintWriter it_out = (PrintWriter) clientMap.get(it.next());
						int count = 1;
						while (list.size() > count)
						{
							it_out.println("[Server] : " + list.get(count) + " ");
							count++;
						}
						it_out.println();

					} catch (Exception e)
					{
						System.out.println("예외 : " + e);
						e.printStackTrace();
					}
				}
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

		Iterator<String> it = clientMap.keySet().iterator();

		while (it.hasNext())
		{
			try
			{
				PrintWriter it_out = (PrintWriter) clientMap.get(it.next());

				if (name.equals(""))
				{
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				} else if (name.equals(name))
				{

					it_out.println("[" + name + "] : " + msg);

				}

			} catch (Exception e)
			{
				System.out.println("예외 : " + e);
				e.printStackTrace();
			}
		}
	}

	// 접속자수 출력
	public void conUserNamePrint(String name)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);

		Set<String> key = clientMap.keySet();
		it_out.println("현재 접속자");
		for (Iterator<String> iterator = key.iterator(); iterator.hasNext();)
		{
			String keyName = (String) iterator.next();
			it_out.println(keyName);
		}
	}

	// 귓속말
	public void sendOneMsg(String name, String msg)
	{
		try
		{
			ArrayList<String> list = stringTok(msg);
			System.out.println(msg);
			PrintWriter it_out = (PrintWriter) clientMap.get(list.get(1));
			PrintWriter it_out2 = (PrintWriter) clientMap.get(name);
			int count = 2;
			it_out.print("[" + name + " >> " + list.get(1) + "] : ");
			it_out2.print("[" + name + " >> " + list.get(1) + "] : ");
			while (list.size() > count)
			{

				it_out.print(list.get(count) + " ");
				it_out2.print(list.get(count) + " ");
				count++;
			}
			it_out.println();
			it_out2.println();
		} catch (Exception e)
		{
			System.out.println("없는 사용자 입니다.");
		}

	}

	// 블랙리스트 추가
	public void addAndRemoveBlackList(String msg)
	{
		ArrayList<String> list = stringTok(msg);
		if (list.size() == 2)
		{
			if (blackList.contains(list.get(1)) == true)
			{
				blackList.remove(list.get(1));
			} else if (blackList.contains(list.get(1)) == false)
			{
				blackList.add(list.get(1));
			}
		}
		System.out.println(blackList);

	}

	// 클라이언트가 접속 못함
	public void notConnectMsg(String name)
	{
		try
		{
			PrintWriter it_out = (PrintWriter) clientMap.get(name);
			it_out.println("중복된 이름으로 접속이 거부되었습니다.");
		} catch (Exception e)
		{

			System.out.println("notConnectMsg : " + e);
		}

	}

	// 문자열 나누기
	public ArrayList<String> stringTok(String msg)
	{
		StringTokenizer st = new StringTokenizer(msg);
		ArrayList<String> list = new ArrayList<>();
		while (st.hasMoreElements())
		{
			list.add(st.nextToken());
		}
		return list;
	}

	// 명령어 판단
	public void cmdCheck(String name, String msg)
	{
		if (msg.indexOf("/list") != -1)
		{
			System.out.println("/list명령어");

			conUserNamePrint(name);
		} else if (msg.indexOf("/to") != -1)
		{

			System.out.println("/to명령어");
			ArrayList<String> list = stringTok(msg);
			if (list.size() == 2)
			{
				if (whisperClient.containsKey(name) == true)
				{
					System.out.println("고정귓속말 OFF");
					whisperClient.remove(name);
				} else
				{
					System.out.println("고정귓속말 ON");
					whisperClient.put(name, msg);

				}
			} else
			{
				sendOneMsg(name, msg);
			}

		} else if (msg.indexOf("/black") != -1)
		{
			System.out.println("/black명령어");
			addAndRemoveBlackList(msg);
		} else
		{
			return;
		}

	}

	class MultiServerT2 extends Thread
	{
		@Override
		public void run()
		{
			while (true)
			{
				serverNotice();

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
				// 인코딩
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
			} catch (Exception e)
			{
				System.out.println("예외 : " + e);
				e.printStackTrace();
			}
		}

		@Override
		public void run()
		{
			// 클라이언트로부터 전송된 "대화명"을 저장할 변수, 메세지 저장용 변수
			String name = "";
			String s = "";
			Boolean flag = true;

			try
			{
				name = URLDecoder.decode(in.readLine(), "UTF-8");
				flag = db.saveClient(name);
				if (blackList.contains(name) == true)
				{
					db.deleteClient(name);
					flag = false;
				}

			} catch (Exception e1)
			{
				e1.printStackTrace();
			}
			if (flag == true)
			{
				try
				{

					sendAllMsg("", name + "님이 입장하셨습니다.");

					clientMap.put(name, out);

					System.out.println(name + " 접속");
					System.out.println("현재 접속자 수는 " + clientMap.size() + "명 입니다.");

					// 입력한 메세지는 모든 클라이언트에게 Echo된다.
					while (in != null)
					{
						s = URLDecoder.decode(in.readLine(), "UTF-8");
						if (s == null)
							break;

						System.out.println(name + " >> " + s);

						db.saveContent(name, s);

						if (s.indexOf("/") != 0)
						{
							if (whisperClient.containsKey(name) == true)
							{
								System.out.println(whisperClient.keySet());
								sendOneMsg(name, whisperClient.get(name) + " " + s);
							} else
							{
								sendAllMsg(name, s);
							}
						} else if (s.indexOf("/") == 0)
						{
							cmdCheck(name, s);
						}
					}

				} catch (Exception e)
				{
					System.out.println("예외 : " + e);

				} finally
				{
					clientMap.remove(name);
					sendAllMsg("", name + "님이 퇴장하셨습니다.");
					db.deleteClient(name);
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
			} else
			{
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
