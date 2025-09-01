package com.bff.ms.api;

import java.math.BigDecimal;

public class ProductoDto {
  private Long id;
  private String sku;
  private String nombre;
  private Integer stock;
  private BigDecimal precio;
  private Long bodegaId;

  public ProductoDto() {}
  public ProductoDto(Long id, String sku, String nombre, Integer stock, BigDecimal precio, Long bodegaId) {
    this.id = id; this.sku = sku; this.nombre = nombre; this.stock = stock; this.precio = precio; this.bodegaId = bodegaId;
  }
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public BigDecimal getPrecio() { return precio; }
  public void setPrecio(BigDecimal precio) { this.precio = precio; }
  public Long getBodegaId() { return bodegaId; }
  public void setBodegaId(Long bodegaId) { this.bodegaId = bodegaId; }
}

