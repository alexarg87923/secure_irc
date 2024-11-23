public class Main {
	private static IO io = new IO();
    public static void main(String[] args) {
        io.print_buff("Welcome to Wormhole IRC");
		io.print_buff("1. Login");
		io.print_flush("2. SignUp");
		
		try {
			switch(io.get_inp_with_cursor()) {
				case "1":
				Authentication.login();
				break;
				case "2":
				Authentication.signup();
				break;
				default:
				io.print_flush("Invalid Input.");
			}
		} catch (Exception e) {
			io.print_flush("Invalid Input.");
		}

		io.close();
		return;
    }
}
