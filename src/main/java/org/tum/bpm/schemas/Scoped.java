package org.tum.bpm.schemas;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Scoped<IN, SCOPE> {
  private IN wrapped;
  private SCOPE scope;
}
