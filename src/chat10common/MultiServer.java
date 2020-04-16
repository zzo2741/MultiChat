package chat10common;

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

	HashMap<String, PrintWriter> clientMap; // K : 이름 V : 내용
	HashMap<String, String> whisperClient; // K : 이름 V : /to 보낼사람
	HashSet<String> blackList; // 블랙리스트
	HashMap<String, HashSet<String>> clientBlockList; // K : 이름 V : HahSet 차단한 상대
	HashSet<String> badWord; // 금칙어
	HashMap<String, Integer> basicRoom; // K : 방이름 V : 정원
	HashMap<String, HashMap<Object, String>> secretRoom; // K : 방이름 V : (K : 기본방 V : 비밀번호)

	DBQueryCollection db = new DBQueryCollection(); // DB 쿼리문

	public MultiServer()
	{
		clientMap = new HashMap<String, PrintWriter>(); // 클라이언트의 이름과 출력스트림을 저장할 HashMap생성
		whisperClient = new HashMap<>(); // 귓속말 고정한 클라이언트 저장할 HashMap생성
		blackList = new HashSet<>(); // 블랙리스트 저장할 HashSet
		clientBlockList = new HashMap<>(); // 차단 대상 저장할 HashMap
		badWord = new HashSet<>(); // 금칙어 저장 HashSet
		basicRoom = new HashMap<>(); // 대화방을 저장할 HashMap
		secretRoom = new HashMap<>(); // 비밀방을 저장할 HashMap
		Collections.synchronizedMap(clientMap);
		Collections.synchronizedMap(whisperClient);
		Collections.synchronizedSet(blackList);
		Collections.synchronizedSet(badWord);
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

	// 메인메소드 : Server객체를 생성한 후 초기화한다.
	public static void main(String[] args)
	{
		MultiServer ms = new MultiServer();
		ms.init();
	}

	// *문자열 나누기*
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

	// 서버 공지사항
	public void serverNotice(ArrayList<String> list)
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
		System.out.println("[공지사항 전송]");

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

	// 접속자 출력
	public void showConUser(String name)
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

	// 모든 명령어 보여주기
	public void showCommandList(String name)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);

		it_out.println("/list - 접속자 확인");
		it_out.println("/to <username> - 해당 유저에게 귓속말 고정 ON / OFF");
		it_out.println("/to <username> <입력> - 해당 유저에게 귓속말 보내기");
		it_out.println("/black <username> - 서버 블랙리스트 등록 / 삭제");
		it_out.println("/block - 대화상대 차단 리스트 확인");
		it_out.println("/block <username> - 대화상대 차단");
		it_out.println("/block <remove> <username> - 대화상대 차단해체");
		it_out.println("/badword - 금칙어 보기");
		it_out.println("/badword <입력> - 금칙어 추가");
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
			it_out.print("[" + name + " >> " + list.get(1) + "] : ");
			it_out2.print("[" + name + " >> " + list.get(1) + "] : ");
			int count = 2;
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

	// 귓속말 고정 ON OFF
	// 귓속말 고정 On, Off
	public void whisperFixSwitch(String name, String msg)
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

	// 대화상대 차단
	public void clientBlock(String name, String msg)
	{
		ArrayList<String> list = stringTok(msg);

		if (list.size() == 2)
		{
			if (clientBlockList.containsKey(name) == true)
			{
				System.out.println("블락 추가");
				clientBlockList.get(name).add(list.get(1));
			} else if (clientBlockList.containsKey(name) == false)
			{
				System.out.println("생성 & 블락 추가");
				clientBlockList.put(name, new HashSet<>());
				clientBlockList.get(name).add(list.get(1));
			}
		} else if (list.size() == 3 && list.get(1).equals("remove"))
		{
			System.out.println("블락 삭제");
			if (clientBlockList.containsKey(name) == true)
			{
				clientBlockList.remove(name, clientBlockList.get(name).remove(list.get(2)));

				if (clientBlockList.get(name).isEmpty() == true)
				{
					clientBlockList.remove(name);
				}
			}
		} else if (list.size() == 1)
		{
			showBlockList(name);
		}
		System.out.println("블락");
		System.out.println(clientBlockList);
		System.out.println(clientBlockList.get(name));
	}

	// 차단한 사람 보기
	public void showBlockList(String name)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);
		it_out.println(clientBlockList.get(name));

	}

	// 차단된 사람 빼고 보내기
	public void sendAllMsgNoBlock(String name, String msg)
	{

		Iterator<String> it = clientMap.keySet().iterator();

		while (it.hasNext())
		{
			try
			{
				String valName = it.next();
				PrintWriter it_out = (PrintWriter) clientMap.get(valName);
				if (name.equals(""))
				{
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				} else if (name.equals(name))
				{
					if (clientBlockList.isEmpty() == true)
					{
						it_out.println("[" + name + "] : " + msg);
					} else
					{
						if (clientBlockList.containsKey(name) == true)
						{
							if (clientBlockList.get(name).contains(valName) == false)
							{
								it_out.println("[" + name + "] : " + msg);
							}
						} else if (clientBlockList.containsKey(valName) == true)
						{
							if (clientBlockList.get(valName).contains(name) == false)
							{
								it_out.println("[" + name + "] : " + msg);
							}
						} else
						{
							it_out.println("[" + name + "] : " + msg);
						}
					}
				}

			} catch (Exception e)
			{
				System.out.println("예외 : " + e);
				e.printStackTrace();
			}
		}
	}

	// 금칙어 설정
	public void addBadWord(ArrayList<String> list, String name)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);

		if (list.size() == 1 && name.equals("") == false)
		{
			it_out.println(badWord);
		} else if (list.size() >= 2)
		{
			badWord.add(list.get(1));
			System.out.println(badWord);
		} else
		{
			System.out.println("금칙어 :" + badWord);
		}

	}

	// 금칙어가 있는지 판단
	public String badWrodCheck(String msg)
	{
		String msg2 = msg;
		for (Iterator<String> iterator = badWord.iterator(); iterator.hasNext();)
		{
			String badword = (String) iterator.next();

			if (msg2.contains(badword))
			{
				msg2 = "나쁜말포함";
			}
		}
		return msg2;
	}

	// 방만들기
	public void createRoom(String name, String msg)
	{
		ArrayList<String> list = stringTok(msg);
		PrintWriter it_out = (PrintWriter) clientMap.get(name);

		if (list.size() == 3)
		{

		}
	}

	// 명령어 판단
	public void cmdCheck(String name, String msg)
	{
		ArrayList<String> list = stringTok(msg);

		if (msg.indexOf("/list") != -1)
		{
			System.out.println("/list명령어");

			showConUser(name);
		} else if (msg.indexOf("/to") != -1)
		{
			whisperFixSwitch(name, msg);
		} else if (msg.indexOf("/black") != -1)
		{
			System.out.println("/black명령어");
			addAndRemoveBlackList(msg);
		} else if (msg.indexOf("/block") != -1)
		{
			System.out.println("/block명령어");
			clientBlock(name, msg);
		} else if (msg.indexOf("/help") != -1)
		{
			System.out.println("/help명령어");
			showCommandList(name);
		} else if (msg.indexOf("/badword") != -1)
		{
			System.out.println("/badword명령어");
			addBadWord(list, name);
		} else if (msg.indexOf("/create") != -1)
		{
			System.out.println("/create명령어");
			createRoom(name, msg);
		} else
		{
			return;
		}

	}

	// 서버 공지를 위한 스레드
	class MultiServerT2 extends Thread
	{
		@Override
		public void run()
		{
			while (true)
			{
				Scanner scanner = new Scanner(System.in);
				String serverMsg = scanner.nextLine();

				ArrayList<String> list = stringTok(serverMsg);
				if (list.size() >= 1)
				{
					if (list.get(0).equals("/notice"))
					{
						serverNotice(list);
					} else if (list.get(0).equals("/badword"))
					{
						addBadWord(list, "");
					}
				}
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

						s = badWrodCheck(s);

						if (s.indexOf("/") != 0)
						{
							if (whisperClient.containsKey(name) == true)
							{
								System.out.println(whisperClient.keySet());
								sendOneMsg(name, whisperClient.get(name) + " " + s);
							} else
							{
								sendAllMsgNoBlock(name, s);
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
					db.deleteClient(name);
				} catch (Exception e2)
				{
					e2.printStackTrace();
				}

			}

		}
	}
}