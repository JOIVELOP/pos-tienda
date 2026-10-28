CREATE TABLE T_TURNO_CAJA (
    id BIGINT NOT NULL AUTO_INCREMENT,
    folio VARCHAR(20) NOT NULL,
    apertura DATETIME NOT NULL,
    cierre DATETIME NULL,
    fondo_inicial DECIMAL(12,2) NOT NULL,
    efectivo_esperado DECIMAL(12,2) NULL,
    efectivo_contado DECIMAL(12,2) NULL,
    diferencia DECIMAL(12,2) NULL,
    estado VARCHAR(12) NOT NULL,
    id_usuario_apertura BIGINT NOT NULL,
    id_usuario_cierre BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_turno_caja_folio (folio),
    KEY idx_turno_caja_usuario_estado (id_usuario_apertura, estado),
    CONSTRAINT fk_turno_caja_usuario_apertura FOREIGN KEY (id_usuario_apertura)
        REFERENCES T_USUARIO (id),
    CONSTRAINT fk_turno_caja_usuario_cierre FOREIGN KEY (id_usuario_cierre)
        REFERENCES T_USUARIO (id)
) ENGINE=InnoDB;

CREATE TABLE T_MOVIMIENTO_CAJA (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo VARCHAR(16) NOT NULL,
    concepto VARCHAR(20) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    fecha DATETIME NOT NULL,
    id_turno BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_movimiento_caja_turno_fecha (id_turno, fecha),
    CONSTRAINT fk_movimiento_caja_turno FOREIGN KEY (id_turno)
        REFERENCES T_TURNO_CAJA (id),
    CONSTRAINT fk_movimiento_caja_usuario FOREIGN KEY (id_usuario)
        REFERENCES T_USUARIO (id)
) ENGINE=InnoDB;

CREATE TABLE T_COMPRA (
    id BIGINT NOT NULL AUTO_INCREMENT,
    folio VARCHAR(20) NOT NULL,
    fecha DATETIME NOT NULL,
    estado VARCHAR(12) NOT NULL,
    id_proveedor BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_compra_folio (folio),
    KEY idx_compra_fecha (fecha),
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES T_PROVEEDOR (id),
    CONSTRAINT fk_compra_usuario FOREIGN KEY (id_usuario)
        REFERENCES T_USUARIO (id)
) ENGINE=InnoDB;

CREATE TABLE T_LINEA_COMPRA (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_compra BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    cantidad_solicitada INT NOT NULL,
    cantidad_recibida INT NOT NULL,
    costo_unitario DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_linea_compra_compra (id_compra),
    CONSTRAINT fk_linea_compra_compra FOREIGN KEY (id_compra)
        REFERENCES T_COMPRA (id),
    CONSTRAINT fk_linea_compra_producto FOREIGN KEY (id_producto)
        REFERENCES T_PRODUCTO (id)
) ENGINE=InnoDB;

CREATE TABLE T_RECEPCION_COMPRA (
    id BIGINT NOT NULL AUTO_INCREMENT,
    folio VARCHAR(20) NOT NULL,
    fecha DATETIME NOT NULL,
    id_compra BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recepcion_compra_folio (folio),
    KEY idx_recepcion_compra (id_compra, fecha),
    CONSTRAINT fk_recepcion_compra_compra FOREIGN KEY (id_compra)
        REFERENCES T_COMPRA (id),
    CONSTRAINT fk_recepcion_compra_usuario FOREIGN KEY (id_usuario)
        REFERENCES T_USUARIO (id)
) ENGINE=InnoDB;

CREATE TABLE T_LINEA_RECEPCION (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_recepcion BIGINT NOT NULL,
    id_linea_compra BIGINT NOT NULL,
    cantidad_buena INT NOT NULL,
    cantidad_danada INT NOT NULL,
    costo_unitario DECIMAL(10,2) NOT NULL,
    observacion VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_linea_recepcion_recepcion (id_recepcion),
    CONSTRAINT fk_linea_recepcion_recepcion FOREIGN KEY (id_recepcion)
        REFERENCES T_RECEPCION_COMPRA (id),
    CONSTRAINT fk_linea_recepcion_compra FOREIGN KEY (id_linea_compra)
        REFERENCES T_LINEA_COMPRA (id)
) ENGINE=InnoDB;

CREATE TABLE T_DEVOLUCION (
    id BIGINT NOT NULL AUTO_INCREMENT,
    folio VARCHAR(20) NOT NULL,
    fecha DATETIME NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    metodo_reembolso VARCHAR(20) NOT NULL,
    total_reembolsado DECIMAL(12,2) NOT NULL,
    id_venta BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    id_turno_caja BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_devolucion_folio (folio),
    KEY idx_devolucion_venta_fecha (id_venta, fecha),
    CONSTRAINT fk_devolucion_venta FOREIGN KEY (id_venta)
        REFERENCES T_VENTA (id),
    CONSTRAINT fk_devolucion_usuario FOREIGN KEY (id_usuario)
        REFERENCES T_USUARIO (id),
    CONSTRAINT fk_devolucion_turno FOREIGN KEY (id_turno_caja)
        REFERENCES T_TURNO_CAJA (id)
) ENGINE=InnoDB;

CREATE TABLE T_LINEA_DEVOLUCION (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_devolucion BIGINT NOT NULL,
    id_detalle_venta BIGINT NOT NULL,
    cantidad INT NOT NULL,
    reingresa_inventario BIT NOT NULL,
    monto_reembolsado DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_linea_devolucion_devolucion (id_devolucion),
    KEY idx_linea_devolucion_detalle (id_detalle_venta),
    CONSTRAINT fk_linea_devolucion_devolucion FOREIGN KEY (id_devolucion)
        REFERENCES T_DEVOLUCION (id),
    CONSTRAINT fk_linea_devolucion_detalle FOREIGN KEY (id_detalle_venta)
        REFERENCES T_DETALLE_VENTA (id)
) ENGINE=InnoDB;

ALTER TABLE T_VENTA
    ADD COLUMN id_turno_caja BIGINT NULL,
    ADD KEY idx_venta_turno_pago_estado (id_turno_caja, metodo_pago, estatus),
    ADD CONSTRAINT fk_venta_turno_caja FOREIGN KEY (id_turno_caja)
        REFERENCES T_TURNO_CAJA (id);
