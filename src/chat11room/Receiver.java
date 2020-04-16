package chat11room;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;

public class Receiver extends Thread
{
	Socket socket;
	BufferedReader in = null;

	public Receiver(Socket socket)
	{
		this.socket = socket;

		try
		{
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
		} catch (Exception e)
		{
			System.out.println("예외>Receiver>생성자 : " + e);
		}
	}

	@Override
	public void run()
	{
		// 소켓이 종료되면 while()문을 벗어나서 InputStream을 종료한다.
		while (in != null)
		{
			try
			{
				// 서버에서 받아서 출력
//				System.out.println("Thread Receive : " + URLDecoder.decode(in.readLine(), "UTF-8"));
				System.out.println(URLDecoder.decode(in.readLine(), "UTF-8"));
			} catch (SocketException e)
			{
				System.out.println("SocketException이 발생됨");
				break;
			} catch (Exception e)
			{
				System.out.println("접속이 거부된 이름입니다.(중복, 블랙리스트)");
				System.out.println("예외>Receiver>run : " + e);
				break;
			}
		}
		try
		{
			in.close();
		} catch (Exception e)
		{
			System.out.println("예외>Receiver>run2 : " + e);
		}
	}
}
