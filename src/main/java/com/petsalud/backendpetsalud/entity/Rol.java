package com.petsalud.backendpetsalud.entity;

/**
 * Roles definidos por los requerimientos de CLÍNICA PETSALUD.
 * CLIENTE  : dueño de mascotas, solo accede a SUS propios datos.
 * EMPLEADO : veterinarios, recepción y bodega (operación diaria).
 * GERENTE  : administración, reportes, eliminación y gestión de usuarios.
 */
public enum Rol {
    CLIENTE,
    EMPLEADO,
    GERENTE
}
