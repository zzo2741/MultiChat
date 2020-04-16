package chat11room;

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

import javax.jws.Oneway;

public class MultiServer
{
	static ServerSocket serverSocket = null;
	static Socket socket = null;

	HashMap<String, PrintWriter> clientMap; // K : 유저이름 V : 내용
	HashMap<String, String> whisperClient; // K : 유저이름 V : /to 보낼사람
	HashSet<String> blackList; // 블랙리스트
	HashMap<String, HashSet<String>> clientBlockList; // K : 유저이름 V : 차단한 상대
	HashSet<String> badWord; // 금칙어
	HashMap<String, Integer> basicRoom; // K : 방이름 V : 정원
	HashMap<String, ArrayList<Object>> secretRoom; // K : 방이름 V : 정원, 비밀번호
	HashMap<String, HashMap<String, String>> basicRoomsInfo; // K : 방이름 V : (K : 이름 V : /join 대화방이름)
	HashMap<String, HashMap<String, String>> secretRoomsInfo; // K : 방이름 V : (K : 이름 V : /join 대화방이름)
	HashMap<String, String> userInRoom; // K : 유저이름 V : 방이름

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
		basicRoomsInfo = new HashMap<>(); // 방정보를 저장할 HashMap
		secretRoomsInfo = new HashMap<>(); // 방정보를 저장할 HashMap
		userInRoom = new HashMap<>(); // 대화방에 있는 유저 현황을 저장할 HashMap
		Collections.synchronizedMap(clientMap);
		Collections.synchronizedMap(whisperClient);
		Collections.synchronizedMap(basicRoom);
		Collections.synchronizedMap(secretRoom);
		Collections.synchronizedMap(basicRoomsInfo);
		Collections.synchronizedMap(secretRoomsInfo);
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

	// 모든 명령어 보기
	public void showCommandList(String name)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);

		it_out.println("/notice <입력> - 공지사항(서버명령어)");
		it_out.println("/list - 접속자 확인");
		it_out.println("/to <유저이름> - 해당 유저에게 귓속말 고정 ON / OFF");
		it_out.println("/to <유저이름> <입력> - 해당 유저에게 귓속말 보내기");
		it_out.println("/black <유저이름> - 서버 블랙리스트 등록 / 삭제");
		it_out.println("/block - 대화상대 차단 리스트 확인");
		it_out.println("/block <유저이름> - 대화상대 차단");
		it_out.println("/block <remove> <유저이름> - 대화상대 차단해체");
		it_out.println("/badword - 금칙어 보기");
		it_out.println("/badword <입력> - 금칙어 추가");
		it_out.println("/create <방이름> <정원> - 일반대화방 생성");
		it_out.println("/create <방이름> <정원> <비밀번호> - 비밀대화방 생성");
		it_out.println("/roomlist - 방리스트 보기");
		it_out.println("/join <방이름> - 대화방 입장");
	}

	// 귓속말
	public void sendOneMsg(String name, String msg)
	{
		try
		{
			ArrayList<String> list = stringTok(msg);
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

	// 귓속말 고정 ON OFF
	public void whisperFixSwitch(String name, String msg)
	{
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
	public void addAndRemoveBlackList(ArrayList<String> list)
	{
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
	public void clientBlock(String name, ArrayList<String> list)
	{

		// /block 이름 [차단]
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
			// /block remove 이름 [차단해제]
		} else if (list.size() == 3 && list.get(1).equals("remove"))
		{
			System.out.println("블락 삭제");
			if (clientBlockList.containsKey(name) == true)
			{
				clientBlockList.remove(name, clientBlockList.get(name).remove(list.get(2)));

				// 차단한 상대가 아무도 없으면 HashSet삭제
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
						// 차단한 상대에게 메세지 보내지 않음
						if (clientBlockList.containsKey(name) == true)
						{
							if (clientBlockList.get(name).contains(valName) == false)
							{
								it_out.println("[" + name + "] : " + msg);
							}
							// 차단한 상대에게 메세지 받지 않음
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

		// /badword
		if (list.size() == 1 && name.equals("") == false)
		{
			it_out.println("금칙어 :" + badWord);
		}
		// /badword 금칙어
		else if (list.size() >= 2)
		{
			badWord.add(list.get(1));
			System.out.println("[금칙어 저장]");
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
	public void createRoom(String name, ArrayList<String> list)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);
		// /create 방이름 정원
		if (list.size() == 3)
		{
			if (basicRoom.containsKey(list.get(1)) == false)
			{
				basicRoom.put(list.get(1), Integer.parseInt(list.get(2)));
				basicRoomsInfo.put(list.get(1), new HashMap<>());
				System.out.println("[대화방생성]" + basicRoom);
				it_out.println("[대화방생성] 방이름 : " + list.get(1));
			} else
			{
				it_out.println("[이미 존재하는 방]");
				System.out.println("[이미 존재하는 방]");
			}
			// /create 방이름 정원 비밀번호
		} else if (list.size() == 4)
		{
			if (secretRoom.containsKey(list.get(1)) == false)
			{
				secretRoom.put(list.get(1), new ArrayList<>());
				secretRoom.get(list.get(1)).add(list.get(2));
				secretRoom.get(list.get(1)).add(list.get(3));
				secretRoomsInfo.put(list.get(1), new HashMap<>());
				System.out.println("[대화방생성]" + secretRoom);
				it_out.println("[비밀대화방생성] 방이름 : " + list.get(1) + " 비밀번호 : " + list.get(3));
			} else
			{
				it_out.println("[이미 존재하는 방]");
				System.out.println("[이미 존재하는 방]");
			}
		}
	}

	// 대화방 리스트 보기
	public void showRoomList(String name, ArrayList<String> list)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);
		Set<String> broomKeys = basicRoomsInfo.keySet();
		Set<String> sroomKeys = secretRoomsInfo.keySet();

		System.out.println("---일반대화방 리스트---");
		System.out.println(broomKeys);
		System.out.println("---비밀대화방 리스트---");
		System.out.println(sroomKeys);

		it_out.println("==일반대화방==");
		for (String keys : broomKeys)
		{
			it_out.println(keys);
		}
		it_out.println("==비밀대화방==");
		for (String keys : sroomKeys)
		{
			it_out.println(keys);
		}
	}

	// 일반 대화방 입장
	public void joinBRoom(String name, String msg)
	{
		ArrayList<String> list = stringTok(msg);
		PrintWriter it_out = (PrintWriter) clientMap.get(name);

		// /join 방이름
		if (basicRoom.isEmpty() == true)
		{
			it_out.println("[존재하지 않는 방입니다.]");
		} else
		{
			// /join 방이름
			if (list.size() == 2)
			{
				if (basicRoom.containsKey(list.get(1)) == true)
				{
					it_out.println("[" + list.get(1) + "] 입장");
					basicRoomsInfo.get(list.get(1)).put(name, msg);
					userInRoom.put(name, list.get(1));
					System.out.println("대화방에 있는 유저 : " + userInRoom);
					System.out.println("방정보 : " + basicRoomsInfo);
				} else
				{
					it_out.println("[존재하지 않는 방입니다.]");

				}
			}
		}

	}

	// 비밀 대화방 입장
	public void joinSRoom()
	{

	}

	// 명령어 판단
	public void cmdCheck(String name, String msg)
	{
		ArrayList<String> list = stringTok(msg);

		if (msg.indexOf("/list") == 0)
		{
			System.out.println("/list명령어");
			showConUser(name);
		} else if (msg.indexOf("/to") == 0)
		{
			whisperFixSwitch(name, msg);
		} else if (msg.indexOf("/black") == 0)
		{
			System.out.println("/black명령어");
			addAndRemoveBlackList(list);
		} else if (msg.indexOf("/block") == 0)
		{
			System.out.println("/block명령어");
			clientBlock(name, list);
		} else if (msg.indexOf("/help") == 0)
		{
			System.out.println("/help명령어");
			showCommandList(name);
		} else if (msg.indexOf("/badword") == 0)
		{
			System.out.println("/badword명령어");
			addBadWord(list, name);
		} else if (msg.indexOf("/create") == 0)
		{
			System.out.println("/create명령어");
			createRoom(name, list);
		} else if (msg.indexOf("/roomlist") == 0)
		{
			System.out.println("/roomlist명령어");
			showRoomList(name, list);
		} else if (msg.indexOf("/join") == 0)
		{
			System.out.println("/join명령어");
			joinBRoom(name, msg);
		} else
		{
			return;
		}

	}

	// 서버 스레드
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
						System.out.println("/notice명령어");
						serverNotice(list);
					} else if (list.get(0).equals("/badword"))
					{
						System.out.println("/badword명령어");
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

						// /로 명령어인지 일반 대화인지 판단
						if (s.indexOf("/") != 0)
						{
							if (whisperClient.containsKey(name) == true)
							{
								System.out.println(whisperClient.keySet());
								sendOneMsg(name, whisperClient.get(name) + " " + s);
							}
//							else if (basicRoomsInfo.containsKey(name) == true)
//							{
//
//							} 
							else
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
					e.printStackTrace();

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