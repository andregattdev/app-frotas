package com.andregattermeyer.api_frota.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.andregattermeyer.api_frota.exception.BadRequestException;

class FotoParadaStorageServiceTest {

    @TempDir
    Path diretorio;

    @Test
    void rejectsMissingAndInvalidImage() {
        FotoParadaStorageService storage = new FotoParadaStorageService(diretorio.toString());

        assertThrows(BadRequestException.class, () -> storage.validar(null));
        assertThrows(BadRequestException.class, () -> storage.validar(
                new MockMultipartFile("foto", "arquivo.jpg", "image/jpeg", "nao e imagem".getBytes())));
    }

    @Test
    void rejectsImageLargerThanEightMegabytes() {
        FotoParadaStorageService storage = new FotoParadaStorageService(diretorio.toString());
        byte[] bytes = new byte[8 * 1024 * 1024 + 1];
        MockMultipartFile foto = new MockMultipartFile("foto", "foto.png", "image/png", bytes);

        assertThrows(BadRequestException.class, () -> storage.validar(foto));
    }

    @Test
    void validatesStoresAndLoadsPngPhoto() throws IOException {
        FotoParadaStorageService storage = new FotoParadaStorageService(diretorio.toString());
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        BufferedImage original = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = original.createGraphics();
        grafico.setColor(Color.WHITE);
        grafico.fillRect(0, 0, 640, 480);
        grafico.dispose();
        ImageIO.write(original, "png", saida);
        MockMultipartFile foto = new MockMultipartFile("foto", "prova.png", "image/png", saida.toByteArray());

        String tipo = storage.validar(foto);
        String nome = storage.armazenar(foto, tipo, LocalDateTime.of(2026, 10, 4, 16, 30), -23.550520, -46.633308);
        BufferedImage salva = ImageIO.read(storage.carregar(nome).getFile());

        assertEquals("image/png", tipo);
        assertEquals("image/png", storage.tipoConteudo(nome));
        assertNotNull(storage.carregar(nome));
        assertEquals(Color.WHITE.getRGB(), salva.getRGB(600, 100));
        assertTrue(new Color(salva.getRGB(600, 470)).getRed() < 100);
        assertEquals(1, diretorio.toFile().listFiles().length);
    }

    @Test
    void storesPhotoWithTimestampAndNoGpsCoordinates() throws IOException {
        FotoParadaStorageService storage = new FotoParadaStorageService(diretorio.toString());
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        BufferedImage original = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = original.createGraphics();
        grafico.setColor(Color.WHITE);
        grafico.fillRect(0, 0, 640, 480);
        grafico.dispose();
        ImageIO.write(original, "png", saida);
        MockMultipartFile foto = new MockMultipartFile("foto", "prova.png", "image/png", saida.toByteArray());
        LocalDateTime registradoEm = LocalDateTime.of(2026, 10, 4, 16, 30);

        String nome = storage.armazenar(foto, "image/png", registradoEm, null, null);
        BufferedImage salva = ImageIO.read(storage.carregar(nome).getFile());

        assertEquals(Color.WHITE.getRGB(), salva.getRGB(600, 435));
        assertTrue(new Color(salva.getRGB(600, 470)).getRed() < 100);
    }
}
