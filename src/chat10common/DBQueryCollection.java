package chat10common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DBQueryCollection
{
	private String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
	private String ORALE_URL = "jdbc:oracle:thin://@localhost:1521:orcl";
	private Connection con;

	// DB접속
	public void connectDB()
	{
		try
		{
			String user = "kosmo";
			String pass = "1234";

			Class.forName(ORACLE_DRIVER);
			con = DriverManager.getConnection(ORALE_URL, user, pass);
			System.out.println("DB접속 성공");
		} catch (Exception e)
		{
			System.out.println("DB접속 실패");
			e.printStackTrace();
		}
	}

	// 대화내용 DB에 저장
	public void saveContent(String name, String msg)
	{
		PreparedStatement psmt = null;
		try
		{

			String insertQuery = "INSERT INTO chating_tb VALUES (seq_chating_num.NEXTVAL, ?, ?, ?)";
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date time = new Date();
			String time1 = format.format(time);
			psmt = con.prepareStatement(insertQuery);
			psmt.setString(1, name);
			psmt.setString(2, msg);
			psmt.setString(3, time1);
			psmt.executeUpdate();
			System.out.println("[DB] 대화내용 저장 성공");
		} catch (Exception e)
		{
			System.out.println("[DB] 대화내용 저장 실패");
			e.printStackTrace();
		} finally
		{
			if (psmt != null)
			{
				try
				{
					psmt.close();
				} catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	// 클라이언트 DB에 저장
	public Boolean saveClient(String name)
	{
		PreparedStatement psmt = null;
		boolean flag = true;
		try
		{
			String insertQuery = "INSERT INTO chating_user_tb VALUES (seq_chating_user_num.NEXTVAL, ?, ?)";
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date time = new Date();
			String time1 = format.format(time);
			psmt = con.prepareStatement(insertQuery);
			psmt.setString(1, name);
			psmt.setString(2, time1);
			psmt.executeUpdate();
			System.out.println("[DB] 클라이언트 저장 성공");
		} catch (Exception e)
		{
			System.out.println("[DB에러] 중복된 이름 저장 시도");
			flag = false;
		} finally
		{
			try
			{
				if (psmt != null)
					psmt.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		return flag;

	}

	// 클라이언트 DB에서 삭제
	public void deleteClient(String name)
	{
		PreparedStatement psmt = null;

		try
		{

			String insertQuery = "DELETE FROM chating_user_tb WHERE NICKNAME = ?";
			psmt = con.prepareStatement(insertQuery);
			psmt.setString(1, name);
			psmt.executeUpdate();
			System.out.println("[DB] 클라이언트 삭제 성공");
		} catch (Exception e)
		{
			System.out.println("삭제 대상 업슴");
		} finally
		{
			try
			{
				if (psmt != null)
					psmt.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	// DB연결 끊기
	public void disConnectCB()
	{
		if (con != null)
		{
			try
			{
				con.close();
			} catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

}
