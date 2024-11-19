package com.maple.router.filter;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

  private Long userId;
  private Long organizationId;
  private List<String> roles;
}
