package mente.nova.mente_nova;

import java.io.IOException;
import java.util.Scanner;

import mente.nova.mente_nova.minio.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MenteNovaApplication implements CommandLineRunner {

    @Autowired
    private MinioApplication Minio;
    
	static Scanner input = new Scanner(System.in);
	
	public static void main(String[] args) throws IOException {

        SpringApplication.run(MenteNovaApplication.class, args);

	}

    public void run(String... args) throws Exception {
        System.out.print(Minio.list("mente-nova"));
        System.out.println("Введите любую кнопку для выхода");
        input.nextLine();
        Minio.exit();
    }
}
