package controller;

import jakarta.ws.rs.Path;

@Path("demo")
public class DemoController {
  @Path("hello")
  public String hello() {
    return "Hello World";
  }
}
