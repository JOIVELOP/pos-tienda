CREATE TABLE T_SESION_OFFLINE (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    dispositivo VARCHAR(36) NOT NULL,
    id_turno BIGINT NOT NULL UNIQUE,
    vence DATETIME(6) NOT NULL,
    cerrada BIT NOT NULL,
    secuencia BIGINT NOT NULL,
    efectivo DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_offline_turno FOREIGN KEY (id_turno) REFERENCES T_TURNO_CAJA(id)
) ENGINE=InnoDB;

CREATE TABLE T_CATALOGO_OFFLINE (
    id_sesion VARCHAR(36) NOT NULL,
    id_producto BIGINT NOT NULL,
    codigo VARCHAR(255),
    nombre VARCHAR(255) NOT NULL,
    precio_venta DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    UNIQUE KEY uk_catalogo_offline (id_sesion, id_producto),
    CONSTRAINT fk_catalogo_sesion FOREIGN KEY (id_sesion) REFERENCES T_SESION_OFFLINE(id),
    CONSTRAINT fk_catalogo_producto FOREIGN KEY (id_producto) REFERENCES T_PRODUCTO(id)
) ENGINE=InnoDB;

CREATE TABLE T_OPERACION_OFFLINE (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    id_sesion VARCHAR(36) NOT NULL,
    secuencia BIGINT NOT NULL,
    huella VARCHAR(64) NOT NULL,
    captura DATETIME(6) NOT NULL,
    sincronizacion DATETIME(6) NOT NULL,
    folio VARCHAR(30) NOT NULL,
    UNIQUE KEY uk_operacion_secuencia (id_sesion, secuencia),
    CONSTRAINT fk_operacion_sesion FOREIGN KEY (id_sesion) REFERENCES T_SESION_OFFLINE(id)
) ENGINE=InnoDB;
