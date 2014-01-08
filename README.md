DNSSHIM
=======

Introduction
------------

DNSSHIM is an open-source software that implements the Domain Name
Name System (DNS) protocol for the Internet. Its main feature is to
work as a Hidden Master nameserver, that is, provide information only
to authoritative slave servers. Furthermore he has the ability to
manage, store and automatically resign zones using the security
extension DNSSEC.


Documentation
-------------

For further information, please check at http://www.registro.br/dnsshim

More detailed instructions about how to install and use DNSSHIM can be
found in the manual_en.pdf. In the document protocol_en.pdf there is
detailed documentation on the protocol used for communication between
DNSSHIM and its clients.

Furthermore, quick instructions on how to get started with DNSSHIM are
in the next session.


Getting Started
---------------

In order to get DNSSHIM running and serving zones quickly, follow
these small steps:

- Set the environment variable DNSSHIM_HOME to the base directory
  where configuration files should be placed.

- Start the Signer by running:

   > $ java -jar dnsshim-signer.jar

- Start the XFRD Server by running:
  
   > $ java -jar dnsshim-xfrd.jar

- Download the pydnsshim client from the DNSSHIM website

- Unpack the pydnsshim client and install it by issuing:

   > $ python setup.py install

- Run the client:

  > $ dnssh.py -s <server_IP>

- Before starting creating zones, an user account must be created:

  > dnssh> add-user -u <username>

- Then, do the login:

  > dnssh> login -u <username>

- Now, we can begin sending commands to DNSSHIM. To create a new zone,
  simply do:

  > dnssh> new-zone -z zone.com.br

- Add the NS records to the zone:

  > dnssh> add-rr -z zone.com.br -o "" -t NS -r "ns1.zone.com.br"
  > dnssh> add-rr -z zone.com.br -o "" -t NS -r "ns1.zone.com.br"

- One can also add some other records to the zone:

  > dnssh> add-rr -z zone.com.br -o "www" -t A -r "192.168.0.1"
  > dnssh> add-rr -z zone.com.br -o "" -t MX -r "mx.zone.com.br"
  > dnssh> add-rr -z zone.com.br -o "www2" -t CNAME -r "www.zone.com.br"

- Every zone should have some slaves server assigned, so we need to
  create a slavegroup with some slaves and assign it to the zone:

  > dnssh> new-slavegroup -g <slavegroup_name>
  > dnssh> add-slave -g <slavegroup_name> -s <slave1_IP>
  > dnssh> add-slave -g <slavegroup_name> -s <slave2_IP>
  > dnssh> assign-slavegroup -g <slavegroup_name> -z zone.com.br

- Create a TSIG key for each of the slave server:

  > dnssh> new-tsig-key -k <keyname> -s <slave1_IP>
  > dnssh> new-tsig-key -k <keyname> -s <slave2_IP>

- Finally, we need to publish the zone:

  > dnssh> pub-zone -z zone.com.br
