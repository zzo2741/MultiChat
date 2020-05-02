package test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBConnect {
	
	public PreparedStatement psmt;
	public Connection con;
	public ResultSet rs;
	
	public DBConnect() {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
			String url = "jdbc:oracle:thin://@localhost:1521:orcl";
			String userid = "kosmo";
			String userpw = "1234";
			con = DriverManager.getConnection(url, userid, userpw);
			System.out.println("연결성공");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("연결오류");
		}	
	}
	
	public void close() {
		try {
			if(con!=null) con.close();
			if(psmt!=null) psmt.close();
			if(rs!=null) rs.close();
			System.out.println("자원 반납 완료");
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println("자원반납오류");
		}
	}
	
	public static void main(String[] args) {
		new DBConnect().close();
	}
}