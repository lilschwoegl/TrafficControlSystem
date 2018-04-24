package clientserver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

public class QueryMessage implements Serializable {

	ArrayList<String> columnNames;
	ArrayList<ArrayList<String>> rows;
	
	public QueryMessage()
	{
		columnNames = new ArrayList<String>();
		rows = new ArrayList<ArrayList<String>>();
	}
	
	public void setColumns(String ... cols)
	{
		for (String s : cols)
			columnNames.add(s);
	}
	
	public String[] getColumns()
	{
		return columnNames.toArray(new String[0]);
	}
	
	public String[][] getRows()
	{
		String[][] rowsArr = new String[rows.size()][];
		
		for (int i = 0; i < rows.size(); i++)
		{
			rowsArr[i] = rows.get(i).toArray(new String[0]);
		}
		
		return rowsArr;
	}
	
	public void addRow(String ... rowVals)
	{
		ArrayList<String> v = new ArrayList<String>();
		for (String s : rowVals)
			v.add(s);
		
		rows.add(v);
	}
}
