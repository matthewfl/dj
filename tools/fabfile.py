from fabric.api import *
import os
import uuid
import hashlib
import time

code = os.environ.get('DJ_CODE', uuid.uuid4().hex)
print 'using code: {}'.format(code)

target = 'dj-assembly-0.0.1.jar'

program_jar = os.environ.get('PROGRAM_JAR', None)
main_cls = os.environ.get('MAIN_CLS', 'testcase.SimpleSerializationTest')
djit_cls = os.environ.get('DJIT_CLS', 'edu.berkeley.dj.jit.SimpleJIT')

def short_hash(h):
    return hashlib.md5(h).hexdigest()[:10]

def short_uid():
    return uuid.uuid4().hex[:6]

@task
@parallel
def host_type():
    print env.host
    run('uname -a')


@task
@parallel
def copy():
    # TODO: maybe use rsync here
    put('../target/scala-2.11/{}'.format(target), '/tmp/')
    # if program_jar is not None:
    #     put(program_jar, '/tmp/')


@parallel
def start_client():
    run("tmux new -d -s dj-session-{hash} 'java -Xmx2g -Ddj.cluster_seed={hosts} -jar /tmp/{target} -mode client -cluster_conn hazelcast -cluster_code {code} 2>&1 > /tmp/dj-log-{short_code}-{hash}' > /dev/null".format(
        target=target,
        #id=env.hosts.index(env.host),
        hosts=env.dj_hosts,
        code=code,
        hash=short_uid(),
        short_code=code[:6]
    ))

def start_master():
    run("tmux new -d -s dj-session-master 'java -Xmx2g -Ddj.cluster_seed={hosts} -jar /tmp/{target} -mode master -cluster_conn hazelcast -cluster_code {code} -cp /tmp/{program_jar} -maincls {main_cls} -djit {djit_cls} -debug_clazz_bytecode /tmp/djcls/ 2>&1 > /tmp/dj-log-master-{short_code}-{hash}' > /dev/null".format(
        target=target,
        code=code,
        hosts=env.dj_hosts,
        program_jar=program_jar.split('/')[-1] if program_jar is not None else target,
        main_cls=main_cls,
        djit_cls=djit_cls,
        hash=short_uid(),
        short_code=code[:6]
    ))

@task
@runs_once
def start_remote():
    env.dj_hosts = ','.join([h.split('@')[-1] for h in env.hosts])
    master_r = execute(start_master, hosts=[env.hosts[0]])
    time.sleep(4)
    client_r = execute(start_client, hosts=env.hosts[1:])


@task
@runs_once
def start():
    env.dj_hosts = ','.join([h.split('@')[-1] for h in env.hosts])
    client_r = execute(start_client, hosts=env.hosts)
    time.sleep(7)
    local("tmux new -d -s dj-session-master 'java -Xmx2g -Ddj.cluster_seed={hosts} -jar ../target/scala-2.11/{target} -mode master -cluster_conn hazelcast -cluster_code {code} -cp ../target/scala-2.11/{target} -maincls {main_cls} -djit {djit_cls} -debug_clazz_bytecode /tmp/djcls/ 2>&1 >> /tmp/dj-log-master' > /dev/null".format(
        target=target,
        code=code,
        hosts=env.dj_hosts,
        #program_jar=program_jar.split('/')[-1] if program_jar is not None else target,
        main_cls=main_cls,
        djit_cls=djit_cls,
        hash=short_uid(),
        short_code=code[:6]
    ))


def stop_master():
    run('tmux kill-session -t dj-session-master')

@task
@runs_once
def stop():
    local('tmux kill-session -t dj-session-master')

@task
@runs_once
def stop_remote():
    execute(stop_master, hosts=[env.hosts[0]])


@task
@parallel
def tail():
    run('tail -f `ls -1t /tmp/dj-log* | head -n 1`')
