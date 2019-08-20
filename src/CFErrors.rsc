module CFErrors

import util::ResourceMarkers;
import Message;
import IO;
import String;

public loc LOG = |home:///git/vallang/LOG11|;

int offset(loc file, int l) 
  = ( 0 | it + size(line) | line <- readFileLines(file)[..l-1]);

loc path(str path, str l, str c) {
  p = |project://vallang| + path;
  return p(offset(p, toInt(l)) + toInt(c), 1); 
}

set[Message] messages(loc log) 
  = { warning(m + "@<l>, <c>", path(p, l, c)) 
    | /\[ERROR\] .*\/git\/vallang\/<p:.*>:\[<l:[0-9]+>,<c:[0-9]+>\] <m:.*>$/ <- readFileLines(log) 
    };

void setCFerrors() {
  addMessageMarkers(messages(LOG));
}

void clearCFerrors() {
   removeMessageMarkers(|project://vallang/src|);
}