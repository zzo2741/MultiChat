package chat8cmd;

import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Scanner;

// 클라이언트가 입력한 메세지를 서버로 전송해주는 쓰레드 클래스
public class Sender extends Thread
{
	Socket socket;
	PrintWriter out = null;
	String name;

	// 생성자에서 OutputStream을 생성한다.
	public Sender(Socket socket, String name)
	{
		this.socket = socket;
		try
		{
			out = new PrintWriter(this.socket.getOutputStream(), true);
			this.name = name;
		} catch (Exception e)
		{
			System.out.println("예외>Sender>생성자 : " + e);
		}
	}

	@Override
	public void run()
	{
		Scanner scanner = new Scanner(System.in);

		try
		{
			// 클라이언트가 입력한 "대화명"을 서버로 전송한다.
			out.println(URLEncoder.encode(name, "UTF-8")); // 인코딩
			// out.println(name);

			// Q를 입력하기 전까지의 메세지를 서버로 전송한다.
			while (out != null)
			{
				try
				{
					String s = scanner.nextLine();
					if (s.equalsIgnoreCase("Q"))
					{
						break;
					} else
					{
						// 인코딩
						out.println(URLEncoder.encode(s, "UTF-8"));
					}
				} catch (Exception e)
				{
					System.out.println("예외>Sender>run1 : " + e);
				}
			}
			out.close();
			socket.close();
		} catch (Exception e)
		{
			System.out.println("예외>Sender>run2 : " + e);
		}
	}
}
