package chat9connect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

class test
{

	public static void main(String[] args)
	{
		String listCmd = "/list";
		if (listCmd.indexOf("listAll") != -1)
		{

			System.out.println("있음");
		} else
		{

			System.out.println("없음");
		}
		HashSet<String> hs = new HashSet<>();
		hs.add("1");
		hs.add("2");
		hs.add("3");
		hs.add("4");
		hs.add("5");

		for (Iterator iterator = hs.iterator(); iterator.hasNext();)
		{
			String string = (String) iterator.next();

			if (string.equals("2"))
			{
				System.out.println("바보");
			}
		}

		HashMap<String, String> hm = new HashMap<>();
		hm.put("바보", "바보는 정연");
		hm.put("바보1", "바보는 정연1");
		hm.put("바보2", "바보는 정연2");
		String vn = hm.get("바보");
		System.out.println(vn);

		Set key = hm.keySet();

		for (Iterator<String> iterator = key.iterator(); iterator.hasNext();)
		{
			String keyName = (String) iterator.next();
			String valueName = (String) hm.get(keyName);

			System.out.println(keyName + " = " + valueName);
		}

		String str = "1101,한송이,45,67,89,100";
		StringTokenizer st = new StringTokenizer(str, ",");
		ArrayList<String> list = new ArrayList<>();

		while (st.hasMoreElements())
		{
			list.add(st.nextToken());
		}
		for (Iterator iterator = list.iterator(); iterator.hasNext();)
		{
			String string = (String) iterator.next();
			System.out.println(list.indexOf(string));
			System.out.print(" ");
			System.out.println(string);
		}

	}

}
