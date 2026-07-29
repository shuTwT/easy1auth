package com.easy1auth.security;
import org.springframework.stereotype.Component; import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.security.*; import java.time.Instant; import java.util.*;
@Component public final class TotpService {
 private static final char[] BASE32="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray(); private final SecureRandom random=new SecureRandom();
 public String secret(){byte[] bytes=new byte[20];random.nextBytes(bytes);StringBuilder out=new StringBuilder();int buffer=0,bits=0;for(byte v:bytes){buffer=(buffer<<8)|(v&255);bits+=8;while(bits>=5){out.append(BASE32[(buffer>>(bits-=5))&31]);}}if(bits>0)out.append(BASE32[(buffer<<(5-bits))&31]);return out.toString();}
 public long step(Instant now){return now.getEpochSecond()/30;}
 public boolean verify(String secret,String code,Instant now,Long lastUsed){if(code==null||!code.matches("\\d{6}"))return false;long current=step(now);for(long s=current-1;s<=current+1;s++)if((lastUsed==null||s>lastUsed)&&code(secret,s).equals(code))return true;return false;}
 public String code(String secret,long step){try{byte[] key=decode(secret);byte[] data=new byte[8];for(int i=7;i>=0;i--){data[i]=(byte)step;step>>>=8;}Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(key,"HmacSHA1"));byte[] hash=mac.doFinal(data);int o=hash[19]&15;int binary=((hash[o]&127)<<24)|((hash[o+1]&255)<<16)|((hash[o+2]&255)<<8)|(hash[o+3]&255);return String.format(Locale.ROOT,"%06d",binary%1_000_000);}catch(GeneralSecurityException ex){throw new IllegalStateException(ex);}}
 private static byte[] decode(String value){int buffer=0,bits=0,pos=0;byte[] out=new byte[value.length()*5/8];for(char ch:value.toUpperCase(Locale.ROOT).toCharArray()){int v="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(ch);if(v<0)continue;buffer=(buffer<<5)|v;bits+=5;if(bits>=8)out[pos++]=(byte)(buffer>>(bits-=8));}return Arrays.copyOf(out,pos);}
}
