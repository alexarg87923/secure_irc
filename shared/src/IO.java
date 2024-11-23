import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class IO {
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
	StringTokenizer st;

	void print_buff(String txt) {
		out.println(txt);
	}

	void print_flush(String txt) {
		out.println(txt);
		out.flush();
	}

	public String get_inp_with_cursor() {
		out.print("> ");
		out.flush();
		try {
			st = new StringTokenizer(br.readLine());
		} catch (Exception e) {
			out.println("Invalid Input.");
			out.flush();
			return "";
		}
		return st.nextToken();
	}

	public void close() {
        out.close();
    }
}
