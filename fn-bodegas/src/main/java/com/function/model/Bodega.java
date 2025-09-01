package com.function.model;

public class Bodega {
  private Long id;
  private String codigo;
  private String nombre;
  private String direccion;

  public Bodega() {}

  public Bodega(Long id, String codigo, String nombre, String direccion) {
    this.id = id; this.codigo = codigo; this.nombre = nombre; this.direccion = direccion;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getCodigo() { return codigo; }
  public void setCodigo(String codigo) { this.codigo = codigo; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
}
