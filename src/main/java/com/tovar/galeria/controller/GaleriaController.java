package com.tovar.galeria.controller;

import com.tovar.galeria.model.Foto;
import com.tovar.galeria.repository.FotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
public class GaleriaController {

    @Autowired
    private FotoRepository fotoRepository;

    private static final String UPLOAD_DIR = "C:\\Users\\tovar\\OneDrive\\Desktop\\Proyecto Galeria\\galeria\\uploads";

    // Página principal - Galería
    @GetMapping("/")
    public String galeria(Model model) {
        List<Foto> fotos = fotoRepository.findAllByOrderByFechaCapturaDesc();
        model.addAttribute("fotos", fotos);
        model.addAttribute("titulo", "Nuestra Historia 💕");
        return "index";
    }

    // Panel admin para subir fotos
    @GetMapping("/admin")
    public String panelAdmin(Model model) {
        return "admin";
    }

    // Subir foto
    @PostMapping("/subir")
    public String subirFoto(@RequestParam("archivo") MultipartFile archivo,
                            @RequestParam("titulo") String titulo,
                            @RequestParam("descripcion") String descripcion,
                            @RequestParam("categoria") String categoria,
                            @RequestParam("fechaCaptura") String fechaCaptura,
                            Model model) {

        try {
            // Crear carpeta si no existe
            java.io.File directorio = new java.io.File(UPLOAD_DIR);
            if (!directorio.exists()) {
                boolean creado = directorio.mkdirs();
                System.out.println("Directorio creado: " + creado + " en " + UPLOAD_DIR);
            }

            // Generar nombre único
            String extension = archivo.getOriginalFilename()
                    .substring(archivo.getOriginalFilename().lastIndexOf("."));
            String nombreArchivo = UUID.randomUUID().toString() + extension;

            // Guardar archivo
            java.io.File archivoDestino = new java.io.File(directorio, nombreArchivo);
            archivo.transferTo(archivoDestino);

            System.out.println("Archivo guardado en: " + archivoDestino.getAbsolutePath());

            // Guardar en BD
            Foto foto = new Foto();
            foto.setTitulo(titulo);
            foto.setDescripcion(descripcion);
            foto.setCategoria(categoria);
            foto.setArchivoNombre(nombreArchivo);

            if (!fechaCaptura.isEmpty()) {
                foto.setFechaCaptura(LocalDateTime.parse(fechaCaptura + "T00:00:00"));
            }

            fotoRepository.save(foto);

            model.addAttribute("mensaje", "¡Foto guardada! ❤️");

        } catch (IOException e) {
            model.addAttribute("error", "Error al subir: " + e.getMessage());
            e.printStackTrace();
        }

        return "admin";
    }

    // Servir imágenes
    @GetMapping("/uploads/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> servirImagen(@PathVariable String filename) {
        try {
            Path file = Paths.get(UPLOAD_DIR).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Marcar como favorita
    @PostMapping("/favorita/{id}")
    public String marcarFavorita(@PathVariable Long id) {
        fotoRepository.findById(id).ifPresent(foto -> {
            foto.setEsFavorita(!foto.getEsFavorita());
            fotoRepository.save(foto);
        });
        return "redirect:/";
    }

    // Eliminar foto
    @PostMapping("/eliminar/{id}")
    public String eliminarFoto(@PathVariable Long id) {
        fotoRepository.findById(id).ifPresent(foto -> {
            // Eliminar archivo físico
            java.io.File archivo = new java.io.File(UPLOAD_DIR, foto.getArchivoNombre());
            if (archivo.exists()) {
                archivo.delete();
            }
            // Eliminar de la base de datos
            fotoRepository.delete(foto);
        });
        return "redirect:/";
    }

    // Página de edición
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        Foto foto = fotoRepository.findById(id).orElse(null);
        if (foto == null) {
            return "redirect:/";
        }
        model.addAttribute("foto", foto);
        return "editar";
    }

    // Guardar edición
    @PostMapping("/editar/{id}")
    public String guardarEdicion(@PathVariable Long id,
                                 @RequestParam("titulo") String titulo,
                                 @RequestParam("descripcion") String descripcion,
                                 @RequestParam("categoria") String categoria,
                                 @RequestParam(value = "nuevaFoto", required = false) MultipartFile nuevaFoto,
                                 Model model) {

        fotoRepository.findById(id).ifPresent(foto -> {
            // Actualizar datos
            foto.setTitulo(titulo);
            foto.setDescripcion(descripcion);
            foto.setCategoria(categoria);

            // Si subió nueva foto, reemplazar
            if (nuevaFoto != null && !nuevaFoto.isEmpty()) {
                try {
                    // Eliminar foto anterior
                    java.io.File archivoAnterior = new java.io.File(UPLOAD_DIR, foto.getArchivoNombre());
                    if (archivoAnterior.exists()) {
                        archivoAnterior.delete();
                    }

                    // Guardar nueva foto
                    String extension = nuevaFoto.getOriginalFilename()
                            .substring(nuevaFoto.getOriginalFilename().lastIndexOf("."));
                    String nombreArchivo = UUID.randomUUID().toString() + extension;
                    java.io.File archivoNuevo = new java.io.File(UPLOAD_DIR, nombreArchivo);
                    nuevaFoto.transferTo(archivoNuevo);

                    foto.setArchivoNombre(nombreArchivo);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            fotoRepository.save(foto);
        });

        return "redirect:/";
    }
}