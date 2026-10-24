package com.utp.gajog.controller;

import com.utp.gajog.exception.RecursoNoEncontradoException;
import com.utp.gajog.exception.ReglaNegocioException;
import com.utp.gajog.model.RolUsuario;
import com.utp.gajog.service.AuditoriaService;
import com.utp.gajog.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class UsuarioWebController {

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public UsuarioWebController(UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/cuenta/cambiar-password")
    public String vistaCambiarPassword(
            @RequestParam(defaultValue = "false") boolean forzado,
            Model model
    ) {
        model.addAttribute("forzado", forzado);
        return "cambiar-password";
    }

    @PostMapping("/cuenta/cambiar-password")
    public String procesarCambioPassword(
            @RequestParam String passwordActual,
            @RequestParam String passwordNueva,
            @RequestParam String confirmacionPassword,
            Authentication auth,
            RedirectAttributes redirectAttributes
    ) {
        try {
            usuarioService.cambiarPasswordPropia(auth.getName(), passwordActual, passwordNueva, confirmacionPassword);
            redirectAttributes.addFlashAttribute("exito", "Contraseña actualizada correctamente.");
            return "redirect:/";
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cuenta/cambiar-password";
        }
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/usuarios")
    public String vistaUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodosConPersona());
        model.addAttribute("roles", List.of(RolUsuario.ADMINISTRADOR, RolUsuario.RECEPCION, RolUsuario.INSTRUCTOR));
        model.addAttribute("eventosAuditoria", auditoriaService.listarUltimosEventos());
        return "usuarios";
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/usuarios/guardar")
    public String crearUsuario(
            @RequestParam String dni,
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam String username,
            @RequestParam String passwordInicial,
            @RequestParam RolUsuario rol,
            RedirectAttributes redirectAttributes
    ) {
        try {
            usuarioService.crearUsuarioSistema(dni, nombres, apellidos, username, passwordInicial, rol);
            redirectAttributes.addFlashAttribute("exito", "Usuario creado exitosamente. Deberá cambiar su clave en su primer ingreso.");
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(
            @PathVariable Long id,
            @RequestParam RolUsuario nuevoRol,
            RedirectAttributes redirectAttributes
    ) {
        try {
            usuarioService.cambiarRolUsuario(id, nuevoRol);
            redirectAttributes.addFlashAttribute("exito", "Rol del usuario actualizado a " + nuevoRol + ".");
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam boolean activo,
            RedirectAttributes redirectAttributes
    ) {
        try {
            usuarioService.cambiarEstadoActivo(id, activo);
            redirectAttributes.addFlashAttribute("exito",
                    activo ? "Cuenta de usuario reactivada y desbloqueada." : "Cuenta de usuario desactivada.");
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}