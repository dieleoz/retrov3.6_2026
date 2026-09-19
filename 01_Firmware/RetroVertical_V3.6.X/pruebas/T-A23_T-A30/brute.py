import warnings; warnings.filterwarnings('ignore')
import numpy as np, struct, random, sys
from collections import Counter, defaultdict
F=np.float32
from emul import strtof, efmt
def ulp(f):
    f=abs(F(f)); return float(np.nextafter(f,F(np.inf))-f)
def f2b(f): return struct.unpack('<I',F(f).tobytes())[0]
random.seed(int(sys.argv[1])); N=int(sys.argv[2])
hist=Counter(); porDec=defaultdict(Counter); hS=Counter(); hP=Counter(); hG=Counter(); worst=[]
for k in range(N):
    dec=random.randint(-9,3)
    v=random.choice((-1,1))*random.uniform(1,10)*10.0**dec
    a='%.8E'%v
    f=strtof(a); b=efmt(f)
    env=F(float(a)); lei=F(float(b))
    e=round(abs(float(lei)-float(env))/ulp(lei),3)
    hist[e]+=1; porDec[dec][e]+=1
    # errores parciales
    ok=F(float(a))  # aprox. al correcto (doble redondeo despreciable)
    hS[abs((f2b(f)&0x7FFFFFFF)-(f2b(ok)&0x7FFFFFFF))]+=1
    hP[abs((f2b(F(float(b)))&0x7FFFFFFF)-(f2b(f)&0x7FFFFFFF))]+=1
    # #G de un valor de ROM (sin strtod): float exacto -> efmt -> app
    g=F(v); eg=round(abs(float(F(float(efmt(g))))-float(g))/ulp(F(float(efmt(g)))),3); hG[eg]+=1
    if e>=5: worst.append((e,a,b))
print('N',N); print('ida y vuelta #S->#G (metrica app):',sorted(hist.items()))
print('strtod ulp:',sorted(hS.items())); print('printf ulp:',sorted(hP.items())); print('#G desde ROM (metrica app):',sorted(hG.items()))
for d in sorted(porDec): print(' dec',d,sorted(porDec[d].items()))
print('peores:',sorted(worst,reverse=True)[:10])
