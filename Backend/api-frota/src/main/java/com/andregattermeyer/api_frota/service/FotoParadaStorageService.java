package com.andregattermeyer.api_frota.service;

import java.awt.image.BufferedImage;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;

@Service
public class FotoParadaStorageService {

    private static final long TAMANHO_MAXIMO = 8L * 1024 * 1024;
    private static final long PIXELS_MAXIMOS = 25_000_000L;
    private static final DateTimeFormatter DATA_MARCA_DAGUA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final Path diretorio;

    public FotoParadaStorageService(@Value("${app.upload.directory:./uploads}") String diretorio) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public String validar(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new BadRequestException("Anexe uma foto para registrar a entrega, retirada ou ocorrência.");
        }
        if (foto.getSize() > TAMANHO_MAXIMO) {
            throw new BadRequestException("A foto deve ter no máximo 8 MB.");
        }
        String tipoConteudo = foto.getContentType();
        if (tipoConteudo == null || !(tipoConteudo.equalsIgnoreCase("image/jpeg")
                || tipoConteudo.equalsIgnoreCase("image/png"))) {
            throw new BadRequestException("Envie uma imagem JPEG ou PNG.");
        }

        try (InputStream entrada = foto.getInputStream();
                ImageInputStream imagem = ImageIO.createImageInputStream(entrada)) {
            if (imagem == null) {
                throw new BadRequestException("O arquivo enviado não é uma imagem válida.");
            }
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(imagem);
            if (!leitores.hasNext()) {
                throw new BadRequestException("O arquivo enviado não é uma imagem JPEG ou PNG válida.");
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(imagem, true, true);
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                if (!(formato.equals("jpeg") || formato.equals("jpg") || formato.equals("png"))) {
                    throw new BadRequestException("Envie uma imagem JPEG ou PNG.");
                }
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura <= 0 || altura <= 0 || (long) largura * altura > PIXELS_MAXIMOS) {
                    throw new BadRequestException("A resolução da foto excede o limite permitido.");
                }
                BufferedImage validacao = leitor.read(0);
                if (validacao == null) {
                    throw new BadRequestException("O arquivo enviado não é uma imagem válida.");
                }
                return formato.equals("png") ? "image/png" : "image/jpeg";
            } finally {
                leitor.dispose();
            }
        } catch (IOException erro) {
            throw new BadRequestException("Não foi possível ler a imagem enviada.");
        }
    }

    public String armazenar(
            MultipartFile foto,
            String tipoConteudo,
            LocalDateTime registradoEm,
            Double latitude,
            Double longitude) {
        String extensao = tipoConteudo.equals("image/png") ? ".png" : ".jpg";
        String nome = UUID.randomUUID() + extensao;
        try {
            Files.createDirectories(diretorio);
            BufferedImage original = ImageIO.read(foto.getInputStream());
            if (original == null) {
                throw new BadRequestException("Não foi possível processar a foto enviada.");
            }
            BufferedImage marcada = incluirMarcaDagua(original, registradoEm, latitude, longitude);
            String formato = tipoConteudo.equals("image/png") ? "png" : "jpeg";
            if (!ImageIO.write(marcada, formato, caminhoSeguro(nome).toFile())) {
                throw new IllegalStateException("Não foi possível codificar a foto da parada.");
            }
            return nome;
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível salvar a foto da parada.", erro);
        }
    }

    private BufferedImage incluirMarcaDagua(
            BufferedImage original, LocalDateTime registradoEm, Double latitude, Double longitude) {
        int tipo = original.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage marcada = new BufferedImage(original.getWidth(), original.getHeight(), tipo);
        Graphics2D grafico = marcada.createGraphics();
        try {
            grafico.drawImage(original, 0, 0, null);
            grafico.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            grafico.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int tamanhoFonte = Math.max(12, Math.min(24, original.getWidth() / 40));
            grafico.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamanhoFonte));
            boolean possuiCoordenadas = latitude != null && longitude != null;
            int alturaFaixa = tamanhoFonte * (possuiCoordenadas ? 2 : 1) + 18;
            int topo = Math.max(0, original.getHeight() - alturaFaixa);
            grafico.setComposite(AlphaComposite.SrcOver.derive(0.78f));
            grafico.setColor(Color.BLACK);
            grafico.fillRect(0, topo, original.getWidth(), original.getHeight() - topo);
            grafico.setComposite(AlphaComposite.SrcOver);
            grafico.setColor(Color.WHITE);
            String dataHora = "Registrado em: " + registradoEm.format(DATA_MARCA_DAGUA);
            grafico.drawString(dataHora, 10, Math.min(original.getHeight() - 2, topo + tamanhoFonte + 4));
            if (possuiCoordenadas) {
                String coordenadas = String.format(Locale.ROOT, "GPS: %.6f, %.6f", latitude, longitude);
                grafico.drawString(coordenadas, 10, Math.min(original.getHeight() - 2, topo + tamanhoFonte * 2 + 10));
            }
        } finally {
            grafico.dispose();
        }
        return marcada;
    }

    public Resource carregar(String nome) {
        Path caminho = caminhoSeguro(nome);
        Resource resource = new FileSystemResource(caminho);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("A foto deste registro não foi encontrada no armazenamento.");
        }
        return resource;
    }

    public void excluir(String nome) {
        try {
            Files.deleteIfExists(caminhoSeguro(nome));
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível remover uma foto que não foi vinculada ao registro.", erro);
        }
    }

    public String tipoConteudo(String nome) {
        return nome.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private Path caminhoSeguro(String nome) {
        if (nome == null || !nome.matches("[a-f0-9-]{36}\\.(jpg|png)")) {
            throw new ResourceNotFoundException("Foto de parada inválida.");
        }
        Path caminho = diretorio.resolve(nome).normalize();
        if (!caminho.startsWith(diretorio)) {
            throw new ResourceNotFoundException("Foto de parada inválida.");
        }
        return caminho;
    }
}
