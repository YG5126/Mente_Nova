package mente.nova.mente_nova;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MenteNovaApplicationTests {

	@Test
	void contextLoads() {
	}

}

/*public static void main(String[] args) {

	MinioServer minioServer = new MinioServer();

	System.out.println("Остановка программы - 0\nЗапуск сервера - 1\nПроверка работы сервера - 2\nОстановка сервера - 3");

	int choice = -1;

	while (choice != 0) {

		choice = input.nextInt();

		switch (choice) {
			case 0:
				input.close();
				minioServer.stopServer();
				System.exit(0);
			case 1:
				minioServer.startServer(false);
				break;
			case 2:
				if (minioServer.isPortInUse(9000)) {
					System.out.println("Сервер работает");
				} else {
					System.out.println("Сервер не работает");
				}
				break;
			case 3:
				minioServer.stopServer();
				break;
			default:
				System.out.println("Ошибка: Некорректный выбор");
				break;
		}
	}
}*/

/*try ( BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ) ) {

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[" + command + "] " + line);
                }
            }*/